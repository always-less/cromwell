package cromwell.backend.impl.volc

import cats.data.Validated
import cats.implicits.catsSyntaxTuple2Semigroupal
import common.validation.ErrorOr.ErrorOr
import cromwell.backend._
import cromwell.backend.async.{ExecutionHandle, FailedNonRetryableExecutionHandle, PendingExecutionHandle}
import cromwell.backend.io.JobPathsWithDocker
import cromwell.backend.standard.{StandardAsyncExecutionActor, StandardAsyncExecutionActorParams, StandardAsyncJob}
import cromwell.core.StandardPaths
import cromwell.core.retry.SimpleExponentialBackoff
import cromwell.services.metadata.CallMetadataKeys
import org.apache.commons.lang3.StringUtils
import wom.values.{ShellQuoteHelper, WomFile, WomGlobFile, WomUnlistedDirectory}
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object VolcAsyncBackendJobExecutionActor {
  val JobIdKey = "volc_job_id"
}

/**
  * Runs a job on a shared backend, with the ability to (abstractly) submit asynchronously, then poll, kill, etc.
  *
  * Abstract workhorse of the shared file system.
  *
  * The trait requires that there exist:
  * - Some unix process to submit jobs asynchronously.
  * - When the job runs, outputs will be written to the same filesystem cromwell is executing.
  *
  * As the job runs, this backend will poll for an `rc` file. The `rc` file should be written after the command
  * completes, or will be written by this trait itself during an abort.
  *
  * In practice instead of extending this trait, most systems requiring a backend can likely just configure a backend in
  * the application.conf using a cromwell.backend.impl.sfs.config.ConfigBackendLifecycleActorFactory
  *
  *
  * NOTE: Although some methods return futures due to the (current) contract in BJEA/ABJEA, this actor only executes
  * during the receive, and does not launch new runnables/futures from inside "receive"... except--
  *
  * The __one__ exception is that the when `poll` is processing a successful return code. Currently processReturnCode
  * is calling into a stub for generating fake hashes. This functionality is TBD, but it is likely that we __should__
  * begin teardown while we return a future with the results, assuming we're still using futures instead of akka-ish
  * messages.
  */
class VolcAsyncBackendJobExecutionActor(override val standardParams: StandardAsyncExecutionActorParams)
    extends BackendJobLifecycleActor
    with StandardAsyncExecutionActor
    with VolcJobCachingActorHelper {

  private val volcClient = VolcClient()

  override type StandardAsyncRunInfo = Any

  override type StandardAsyncRunState = VolcTaskRunState

  /** True if the status contained in `thiz` is equivalent to `that`, delta any other data that might be carried around
    * in the state type.
    */
  override def statusEquivalentTo(thiz: StandardAsyncRunState)(that: StandardAsyncRunState): Boolean = thiz.status ==
    that.status

  override lazy val pollBackOff: SimpleExponentialBackoff = SimpleExponentialBackoff(1.second, 5.minutes, 1.1)

  override lazy val executeOrRecoverBackOff: SimpleExponentialBackoff = SimpleExponentialBackoff(3.seconds, 30.seconds, 1.1)

  lazy val jobPathsWithDocker: JobPathsWithDocker = jobPaths.asInstanceOf[JobPathsWithDocker]

  def jobName: String = s"cromwell_${jobDescriptor.workflowDescriptor.id.shortString}_${jobDescriptor.taskCall.localName}"

  override def dockerImageUsed: Option[String] = None

  /**
    * Localizes the file, run outside of docker.
    */
  // todo 默认先不处理
  override def preProcessWomFile(womFile: WomFile): WomFile = {
    womFile
  }

  //  override lazy val commandDirectory: Path = {
  //    super.commandDirectory
  //  }

  override def execute(): ExecutionHandle = {
    jobPaths.callExecutionRoot.createDirectories()

    writeScriptFile()

    val volcTask = buildVolcTask()

    val submitResp = volcClient.submit(volcTask)
    jobLogger.info("Volc task submit resp: {}", submitResp)

    if (submitResp.rc == 0) {
      PendingExecutionHandle(jobDescriptor, StandardAsyncJob(submitResp.jobId), Some(volcTask), None)
    } else {
      FailedNonRetryableExecutionHandle(submitResp.failedCause.get, Some(submitResp.rc), None)
    }
  }

  private def buildVolcTask(): VolcTask = {

    VolcTask(
      volcConfiguration = volcConfiguration,
      jobDescriptor = jobDescriptor,
      configurationDescriptor = configurationDescriptor,
      jobLogger = jobLogger,
      volcJobPaths = volcJobPaths,
      runtimeAttributes = runtimeAttributes,
      taskWorkDir = null,
      instantiatedCommand = instantiatedCommand
    )
  }

  private def writeScriptFile(): Unit = {
    volcCommandScriptContents match {
      case Validated.Valid(scriptString) => jobPaths.script.write(scriptString)
      case Validated.Invalid(errors) =>
        throw new RuntimeException(
          "Failed to execute job due to: " + errors.toList
            .mkString(", ")
        )
    }
    ()
  }

  /** A bash script containing the custom preamble, the instantiated command, and output globbing behavior. */
  private def volcCommandScriptContents: ErrorOr[String] = {
    val commandString = instantiatedCommand.commandString
    val commandStringAbbreviated = StringUtils.abbreviateMiddle(commandString, "...", abbreviateCommandLength)
    jobLogger.info(s"`$commandStringAbbreviated`")
    tellMetadata(Map(CallMetadataKeys.CommandLine -> commandStringAbbreviated))

    // todo alluxio posix 支持不完善，先切换到用户本地目录，执行完后再copy回executionRootPath
//    val cwd = commandDirectory
    val volcTaskCommandDirectory = "/var/log/cromwell-executions"
    val cwd = jobPaths.workflowPaths.buildPath(volcTaskCommandDirectory)
    val output = jobPaths.callExecutionRoot.resolve("output")
    val rcPath = cwd./(jobPaths.returnCodeFilename)
    updateJobPaths()

    // The standard input redirection gets a '<' that standard output and error do not since standard input is only
    // redirected if requested. Standard output and error are always redirected but not necessarily to the default
    // stdout/stderr file names.
    val stdinRedirection = executionStdin.map("< " + _.shellQuote).getOrElse("")
    // todo
//    val stdoutRedirection = executionStdout.shellQuote
//    val stderrRedirection = executionStderr.shellQuote
    val stdoutRedirection = s"$volcTaskCommandDirectory/stdout".shellQuote
    val stderrRedirection = s"$volcTaskCommandDirectory/stderr".shellQuote
    val rcTmpPath = rcPath.plusExt("tmp")
    val tmpDir = s"$volcTaskCommandDirectory/${StringUtils.substringAfterLast(runtimeEnvironment.tempPath, "/")}"

    val errorOrDirectoryOutputs: ErrorOr[List[WomUnlistedDirectory]] =
      backendEngineFunctions.findDirectoryOutputs(call, jobDescriptor)

    val errorOrGlobFiles: ErrorOr[List[WomGlobFile]] =
      backendEngineFunctions.findGlobOutputs(call, jobDescriptor)

    lazy val environmentVariables = instantiatedCommand.environmentVariables map {
        case (k, v) => s"""export $k="$v""""
      } mkString ("", "\n", "\n")

    val home = jobDescriptor.taskCall.callable.homeOverride.map { _(runtimeEnvironment) }.getOrElse("$HOME")
    val shortId = jobDescriptor.workflowDescriptor.id.shortString
    // Give the out and error FIFO variables names that are unlikely to conflict with anything the user is doing.
    val (out, err) = (s"out$shortId", s"err$shortId")

    val dockerOutputDir = jobDescriptor.taskCall.callable.dockerOutputDirectory map {
        d => s"ln -s $cwd $d"
      } getOrElse ""

    // Only adjust the temporary directory permissions if this is executing under Docker.
    val tmpDirPermissionsAdjustment = if (isDockerRun) s"""chmod 777 "$$tmpDir"""" else ""

    val emptyDirectoryFillCommand: String = configurationDescriptor.backendConfig
      .getAs[String]("empty-dir-fill-command")
      .getOrElse(s"""(
           |# add a .file in every empty directory to facilitate directory delocalization on the cloud
           |cd $cwd
           |find . -type d -exec sh -c '[ -z "$$(ls -A '"'"'{}'"'"')" ] && touch '"'"'{}'"'"'/.file' \\;
           |)""".stripMargin)

    // The `tee` trickery below is to be able to redirect to known filenames for CWL while also streaming
    // stdout and stderr for PAPI to periodically upload to cloud storage.
    // https://stackoverflow.com/questions/692000/how-do-i-write-stderr-to-a-file-while-using-tee-with-a-pipe
    // todo
    (errorOrDirectoryOutputs, errorOrGlobFiles).mapN(
      (directoryOutputs, globFiles) =>
        s"""|#!$jobShell
          |DOCKER_OUTPUT_DIR_LINK
          |mkdir -p ${cwd.pathAsString}
          |mkdir -p $tmpDir
          |cd ${cwd.pathAsString}
          |tmpDir=$tmpDir
          |$tmpDirPermissionsAdjustment
          |export _JAVA_OPTIONS=-Djava.io.tmpdir="$$tmpDir"
          |export TMPDIR="$$tmpDir"
          |export HOME="$home"
          |(
          |cd ${cwd.pathAsString}
          |SCRIPT_PREAMBLE
          |)
          |$out="$${tmpDir}/out.$$$$" $err="$${tmpDir}/err.$$$$"
          |mkfifo "$$$out" "$$$err"
          |touch "$$$out" "$$$err"
          |trap 'rm "$$$out" "$$$err"' EXIT
          |touch $stdoutRedirection $stderrRedirection
          |tee $stdoutRedirection < "$$$out" &
          |tee $stderrRedirection < "$$$err" >&2 &
          |echo ====================== Run Task Command Start ======================
          |(
          |cd ${cwd.pathAsString}
          |ENVIRONMENT_VARIABLES
          |INSTANTIATED_COMMAND
          |) $stdinRedirection > "$$$out" 2> "$$$err"
          |echo $$? > $rcTmpPath
          |echo ====================== Run Task Command End ======================
          |$emptyDirectoryFillCommand
          |(
          |cd ${cwd.pathAsString}
          |SCRIPT_EPILOGUE
          |${globScripts(globFiles)}
          |${directoryScripts(directoryOutputs)}
          |)
          |mv $rcTmpPath $rcPath
          |
          |mkdir -p ${output.pathAsString}
          |cp -a $rcPath ${output.pathAsString}/rc
          |cp -a $stdoutRedirection ${output.pathAsString}/stdout
          |cp -a $stderrRedirection ${output.pathAsString}/stderr
          |
          |echo task exit code: `cat $rcPath`
          |exit `cat $rcPath` # todo
          |""".stripMargin
          .replace("SCRIPT_PREAMBLE", scriptPreamble)
          .replace("ENVIRONMENT_VARIABLES", environmentVariables)
          .replace("INSTANTIATED_COMMAND", commandString)
          .replace("SCRIPT_EPILOGUE", scriptEpilogue)
          .replace("DOCKER_OUTPUT_DIR_LINK", dockerOutputDir)
    )
  }

  lazy val standardPaths: StandardPaths = jobPaths.standardPaths

  override def recover(job: StandardAsyncJob): ExecutionHandle = reconnectToExistingJob(job)

  override def reconnectAsync(job: StandardAsyncJob): Future[ExecutionHandle] = {
    Future.successful(reconnectToExistingJob(job))
  }

  override def reconnectToAbortAsync(job: StandardAsyncJob): Future[ExecutionHandle] = {
    Future.successful(reconnectToExistingJob(job, forceAbort = true))
  }

  private def reconnectToExistingJob(job: StandardAsyncJob, forceAbort: Boolean = false): ExecutionHandle = {
    // To avoid race conditions, check for the rc file after checking if the job is alive.
    jobLogger.info("Try to reconnect existing job: {}", job.jobId)
    isAlive(job) match {
      case Success(true) =>
        // If the job is not done and forceAbort is true, try to abort it
        if (!jobPaths.returnCode.exists && forceAbort) {
          jobLogger.info("Job is not done and forceAbort is true, try to abort it, job id: {}", job.jobId)
          tryAbort(job)
        }
        jobLogger.info("Job is alive, job id: {}", job.jobId)
        PendingExecutionHandle(jobDescriptor, job, None, None)
      case Success(false) =>
        if (jobPaths.returnCode.exists) {
          PendingExecutionHandle(jobDescriptor, job, None, None)
        } else {
          // Could start executeScript(), but for now fail because we shouldn't be in this state.
          FailedNonRetryableExecutionHandle(
            new RuntimeException(
              s"Unable to determine that ${job.jobId} is alive, and ${jobPaths.returnCode} does " +
              s"not exist."
            ),
            None,
            kvPairsToSave = None
          )
        }
      case Failure(f) => FailedNonRetryableExecutionHandle(f, None, kvPairsToSave = None)
    }
  }

  def isAlive(job: StandardAsyncJob): Try[Boolean] = Try {
//    jobLogger.info("========= isAlive")
    val getResp = volcClient.get(job, volcJobPaths, jobLogger)
    getResp.volcTaskInfo match {
      case Some(value) => !value.state.terminal
      case None => false
    }
  }

  override def requestsAbortAndDiesImmediately: Boolean = false

  override def tryAbort(job: StandardAsyncJob): Unit = {
    jobLogger.info("Try abort job: {}", job.jobId)
    volcClient.cancel(job, volcJobPaths, jobLogger)
    ()
  }

  override def pollStatus(handle: StandardAsyncPendingExecutionHandle): VolcTaskRunState = {
//    jobLogger.info("job id is {}", handle.pendingJob.jobId)
    if (jobPaths.returnCode.exists) {
      VolcTaskJobDone
    } else if (handle.pendingJob.jobId.isEmpty) {
      jobLogger.info("Job has no job id, should be submit failed, please check the submit info")
      VolcTaskJobFailed
    } else {
      val getResp = volcClient.get(handle.pendingJob, volcJobPaths, jobLogger)
      getResp.volcTaskInfo match {
        //                case Some(value) => if (value.state.terminal) VolcTaskJobRunning(None) else
        //                VolcTaskJobWaitingForReturnCode(None)
        case Some(value) => value.state
        case None => VolcTaskJobFailed
      }
    }
  }

  override def isTerminal(runStatus: StandardAsyncRunState): Boolean = {
//    jobLogger.info(s"isTerminal is ${runStatus.terminal}")
    runStatus.terminal
  }

  override def mapOutputWomFile(womFile: WomFile): WomFile = {
    super.mapOutputWomFile(womFile)
    //    sharedFileSystem.mapJobWomFile(jobPaths)(womFile)
  }
}
