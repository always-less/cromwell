package cromwell.backend.impl.volc

import com.google.common.base.Charsets
import cromwell.backend.impl.volc.util.{ProcessRunner, YamlUtil}
import cromwell.backend.standard.StandardAsyncJob
import cromwell.backend.validation.SupportStorage.{TosStorageType, VolcStorageYaml}
import cromwell.backend.validation.TaskRoleSpecsValidation.constructTaskRoleSpecs
import cromwell.backend.validation.{FrameworkValidation, StoragesValidation, TaskRoleSpec}
import cromwell.core.logging.JobLogger
import cromwell.core.path.Path
import org.apache.commons.lang3.StringUtils
import spray.json.{DefaultJsonProtocol, RootJsonFormat, _}

import scala.collection.Seq
import java.io.{IOException, OutputStreamWriter}
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.collection.mutable

object VolcClient {
  def apply(): VolcClient = {

    new CommandToolVolcClient
  }
}

trait VolcClient {
  def submit(volcTask: VolcTask): VolcTaskSubmitResponse

  def get(job: StandardAsyncJob, jobPaths: VolcJobPaths, jobLogger: JobLogger): VolcTaskGetResponse

  def cancel(job: StandardAsyncJob, jobPaths: VolcJobPaths, jobLogger: JobLogger): VolcTaskCancelResponse
}

class CommandToolVolcClient extends VolcClient {

  def submit(volcTask: VolcTask): VolcTaskSubmitResponse = {
    val configFilePath = writeTaskConfigYamlFile(volcTask)

    val processRunner = new ProcessRunner(
      Seq("volc", "ml_task", "submit", s"--conf=$configFilePath"),
      volcTask.volcJobPaths.volcSubmitStdout,
      volcTask.volcJobPaths.volcSubmitStderr
    )



    val (rc, process) = processRunner.run()

    volcTask.jobLogger
      .info(
        "CommandToolVolcClient submit: configFilePath: {}, rc: {}, process id: {}",
        configFilePath,
        rc.toString,
        process.pid().toString
      )

    val volcTaskId = if (rc == 0) parseVolcTaskId(volcTask) else ""
    val finalRc = if (volcTaskId.nonEmpty) rc else 2
    val failedCause =
      if (finalRc == 0) None
      else if (volcTaskId.isEmpty && rc == 0) {
        Some(
          new RuntimeException(
            "Parsed empty task id from submit stdout, submit stdout: " + volcTask.volcJobPaths.volcSubmitStdout
              .contentAsString(Charsets.UTF_8)
          )
        )

      }
      //      else Some(new RuntimeException(volcTask.volcJobPaths.volcSubmitStderr.contentAsString(Charsets.UTF_8))
      //      +"\n")
      else Some(new RuntimeException(
        s"""
           |stderr:
           |${volcTask.volcJobPaths.volcSubmitStderr.contentAsString(Charsets.UTF_8)}
           |
           |stdout:
           |${volcTask.volcJobPaths.volcSubmitStdout.contentAsString(Charsets.UTF_8)}
           |""".stripMargin))

    VolcTaskSubmitResponse(
      rc = finalRc,
      jobId = volcTaskId,
      failedCause = failedCause
    )
  }

  class ProcessRunnerForGet(val argv: Seq[Any], val stdoutPath: Path, val stderrPath: Path, logger: JobLogger) {
    def run(): Int = {
      // NOTE: Scala's SimpleProcess.exitValue() calls outputThreads.foreach(_.join()) that apparently blocks until sub
      // processes finish. WE DO NOT WANT TO WAIT FOR SUB PROCESSES TO FINISH, especially when trying to background a
      // job!
      val timeout = 2.toLong
      val processBuilder = new java.lang.ProcessBuilder()
      processBuilder.command(argv.map(_.toString): _*)
      processBuilder.redirectOutput(stdoutPath.toFile)
      processBuilder.redirectError(stderrPath.toFile)
      var rc = false
      var process: Process = null
      try {
        process = processBuilder.start()
        //        val reader = new BufferedReader(new InputStreamReader(process.getInputStream, "UTF-8"))
        val writer = new OutputStreamWriter(process.getOutputStream)
        val state = process.waitFor(timeout, TimeUnit.SECONDS)
        if (!state) {
          writer.write("q\n")
          writer.flush()
        }
        //如果正常应当返回true，超时返回false，表示获取信息失败
        rc = process.waitFor(timeout, TimeUnit.SECONDS)
        if (!rc) 1 else 0
      } catch {
        case ex: IOException =>
          logger.info(s"net work failure ${ex}")
          throw ex
      } finally {
        if (!rc) {
          process.destroy()
          logger.info("destroy the process for rc is 0")
        }
      }
    }


  }


  def get(job: StandardAsyncJob, jobPaths: VolcJobPaths, jobLogger: JobLogger): VolcTaskGetResponse = {
    val processRunner = new ProcessRunnerForGet(
      Seq("volc", "ml_task", "get", s"--id=${
        job.jobId
      }"),
      jobPaths.volcGetStdout,
      jobPaths.volcGetStderr,
      jobLogger,
    )
    val rc = processRunner.run()
    VolcTaskGetResponse(
      rc = rc,
      volcTaskInfo = parseVolcTaskGetInfo(job, jobPaths, jobLogger)
    )
  }

  def cancel(job: StandardAsyncJob, jobPaths: VolcJobPaths, jobLogger: JobLogger): VolcTaskCancelResponse = {

    val processRunner = new ProcessRunner(
      Seq("volc", "ml_task", "cancel", s"--id=${
        job.jobId
      }"),
      jobPaths.volcCancelStdout,
      jobPaths.volcCancelStderr
    )
    val (rc, _) = processRunner.run()
    jobLogger.info("CommandToolVolcClient cancel: job id: {}", job.jobId)

    VolcTaskCancelResponse(
      rc = rc
    )
  }

  private def writeTaskConfigYamlFile(volcTask: VolcTask): String = {
    // volcTask.jobLogger.info("======== writeTaskConfigYamlFile, {}", volcTask.runtimeAttributes)
    val runtimeAttrs = volcTask.runtimeAttributes

    def buildTaskName: String = {
      Seq(
        "cromwell",
        volcTask.jobDescriptor.workflowDescriptor.id.shortString,
        volcTask.jobDescriptor.key.tag
          .replaceAll("\\.", "-")
          .replaceAll(":", "-")
      ).mkString("-")
    }

    def buildEntrypoint: String = {
      // alluxio tos 挂载为异步过程，先sleep 30s
      s"sleep 30 && cd ${volcTask.volcJobPaths.callExecutionRoot} && /bin/bash script && sleep 30"
      //      "echo 'hello cromwell'"
    }

    def buildTaskRoleSpecs: Seq[TaskRoleSpecYaml] = {
      val volcTaskRoleSpec = constructTaskRoleSpecs(runtimeAttrs.taskRoleSpecs)
      volcTaskRoleSpec.map(_.map(_.toYaml))
        .getOrElse(
          Seq(TaskRoleSpecYaml("worker", 1, runtimeAttrs.flavor.getOrElse("")))
        )
    }

    def buildStorage: Seq[VolcStorageYaml] = {
      val volcStorage = StoragesValidation.constructVolcStorage(runtimeAttrs.storages)
      volcStorage.map(_.map(_.toYaml)).getOrElse[Seq[VolcStorageYaml]](Seq.empty) ++ Seq(taskExecutionStorage)
      //      Seq(VolcStorageYaml("Tos", "/data00", "cromwell-test"))
    }

    def taskExecutionStorage: VolcStorageYaml = {
      val serverTosStorage = volcTask.volcConfiguration.tos.volcTosStorage
      TosStorageType(
        serverTosStorage.`type`,
        volcTask.volcJobPaths.callExecutionRoot.pathAsString,
        serverTosStorage.bucket,
        StringUtils.substringAfter(volcTask.volcJobPaths.callExecutionRoot.pathAsString, serverTosStorage.mountPath)
      ).toYaml
    }

    // todo 帮用户自动挂载一个data路径
    //    def taskDataStorage: VolcStorageYaml = {
    //      val serverTosStorage = volcTask.volcConfiguration.tos.volcStorage
    //      VolcStorage(
    //        serverTosStorage.`type`,
    //        "/cromwell-data",
    //        serverTosStorage.bucket,
    //        "cromwell-data"
    //      ).toYaml
    //    }

    val taskConfigYaml = TaskConfigYaml(
      TaskName = buildTaskName,
      Description = runtimeAttrs.description.getOrElse(""),
      Entrypoint = buildEntrypoint,
      Tags = runtimeAttrs.tags,
      Envs = runtimeAttrs.envs,
      Image = runtimeAttrs.image.getOrElse(""),
      ImageUrl = runtimeAttrs.imageUrl,
      ImageCredential = runtimeAttrs.imageCredential.map(_.toYaml),
      SidecarImage = runtimeAttrs.sidecarImage,
      ResourceQueueID = runtimeAttrs.resourceQueueId.getOrElse(""), // todo 可以考虑搞个全局queue配置
      Framework = runtimeAttrs.framework.getOrElse(FrameworkValidation.default).value,
      TaskRoleSpecs = buildTaskRoleSpecs,
      ActiveDeadlineSeconds = runtimeAttrs.activeDeadlineSeconds.getOrElse(86400), // todo 用户自定义？
      EnableTensorboard = runtimeAttrs.enableTensorboard.getOrElse(false),
      SidecarMemoryRatio = runtimeAttrs.sidecarMemoryRatio.getOrElse(0.1),
      Storages = buildStorage
    )

    val configPath = volcTask.volcJobPaths.callExecutionRoot.resolve("volc_task_config.yaml")
    volcTask.jobLogger.info("TaskConfigYaml: {}", taskConfigYaml)
    YamlUtil.toYamlString(taskConfigYaml) match {
      case Left(value) => throw new RuntimeException(s"Failed to parse volc task config yaml: ${
        value.message
      }, ${
        value.underlying
      }")
      case Right(value) =>
        volcTask.jobLogger.info(value) // todo to be removed
        configPath.write(value)
    }

    configPath.pathAsString
  }

  private def parseVolcTaskId(volcTask: VolcTask): String = {
    val submitStr = volcTask.volcJobPaths.volcSubmitStdout.contentAsString(Charsets.UTF_8)
    val reg = "创建任务成功,task_id=[a-z0-9-]+".r
    reg.findFirstIn(submitStr) match {
      case None => ""
      case Some(s) => s.trim.substring(15)
    }
  }

  //  private def parseSubmitExitCode(volcTask: VolcTask): Int = {
  //    val submitStr = volcTask.volcJobPaths.volcSubmitStdout.contentAsString(Charsets.UTF_8)
  //    val reg = "volc ml_task: exit status [0-9]+".r
  //    reg.findFirstIn(submitStr) match {
  //      case None => 1
  //      case Some(c) => c.toInt
  //    }
  //  }

  private def setRoleSpecs(roleSpec: String): TaskRoleSpec = {
    //    println(s"roleSpec ${roleSpec}")
    val array = roleSpec.trim.split("\\s+")
    //    println(s"roleSpec array ")
    //    array.foreach(println)
    TaskRoleSpec(array(0), array(2).toInt, array(1))
  }

  private def parseVolcTaskGetInfo(job: StandardAsyncJob, jobPaths: VolcJobPaths, jobLogger: JobLogger)
  : Option[VolcTaskInfo] = {
    val s = jobPaths.volcGetStdout.contentAsString(Charsets.UTF_8)
    try {
      //    volcTask.jobLogger.info("-----parseVolcTaskGetInfo: {}", s)
      val pattern1 = "([\\W]2m)([a-zA-Z]+):[\\W]\\[0m[ ]+([\\s\\S]+?)[\\n]*[\\W]{2}2K".r
      val patternForRoleSpecs = "([\\W]2m)(RoleSpecs):[\\W]\\[0m[ ]+[\\s\\S]+?2K[\\s][ ]+([\\s\\S]+?)[\\W]{2}2K".r
      val taskDetailMap = mutable.HashMap[String, String]()

      for (taskDetail <- pattern1.findAllMatchIn(s)) {
        //      volcTask.jobLogger.info(s"key: ${taskDetail.group(2)} val: ${taskDetail.group(3)}")
        taskDetailMap(taskDetail.group(2)) = taskDetail.group(3)
      }

      val roleSpec = patternForRoleSpecs.findFirstMatchIn(s)
      //    volcTask.jobLogger.info(s"role spec match ${roleSpec}")
      val taskRoleSpec = roleSpec.get.group(3).split("\n").toSeq.map(perRoleSpec =>
        setRoleSpecs(perRoleSpec))
      //      jobLogger.info(s"role spec match $taskRoleSpec")
      //      jobLogger.info(s"current job status is $taskDetailMap")

      val re = Option(VolcTaskInfo(taskDetailMap("Name"), taskDetailMap("Id"), taskDetailMap("Image"), taskDetailMap
      ("CreateTime"), taskDetailMap("LaunchTime"), MapStatusToEnum(taskDetailMap("State"), job, jobPaths), taskDetailMap
      ("EntrypointPath"),
        taskDetailMap("Args").split(" ").toSeq, taskDetailMap("Framework"), taskDetailMap("ExitCode").toInt,
        taskRoleSpec))
      //      jobLogger.info("end of the get volc task info")
      re
    }
    catch {
      case ex: NoSuchElementException =>
        jobLogger.error(s"$ex")
        None
      case ex: Exception =>
        jobLogger.error(s"${ex}")
        None
    }
  }

  val SIGTERM = 143
  val SIGINT = 130
  val SIGKILL = 137

  def MapStatusToEnum(status: String, job: StandardAsyncJob, volcJobPaths: VolcJobPaths): VolcTaskRunState = status
  match {
    case "Success" =>
      writeRCToJobPath(0, volcJobPaths.returnCode)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.output)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.error)
      VolcTaskJobSuccess
    case "Queue" => VolcTaskJobQueue
    case "Cancelled" =>
      writeRCToJobPath(SIGKILL, volcJobPaths.returnCode)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.output)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.error)
      VolcTaskJobCancelled
    case "Killing" => VolcTaskJobKilling
    case "Failed" =>
      writeRCToJobPath(1, volcJobPaths.returnCode)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.output)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.error)
      VolcTaskJobFailed
    case "Running" => VolcTaskJobRunning(Option(Instant.now))
    case "Staging" => VolcTaskJobStaging
    case "Initialized" => VolcTaskJobInitialized
    case "SuccessHolding" =>
      writeRCToJobPath(0, volcJobPaths.returnCode)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.output)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.error)
      VolcTaskJobSuccessHold
    case "FailedHolding" =>
      writeRCToJobPath(1, volcJobPaths.returnCode)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.output)
      writeTaskOutput(status, job, volcJobPaths.standardPaths.error)
      VolcTaskJobFailedHold
  }

  def writeRCToJobPath(rc: Int, rcPath: Path): Unit = {
    rcPath.write(s"$rc\n")(Seq(StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.SYNC))
    //    println(s"the rc code is ${rcPath.exists}")
    ()
  }

  def writeTaskOutput(status: String, job: StandardAsyncJob, outputPath: Path): Unit = {
    // todo
    val volcCustomTaskSite = s"https://console.volcengine.com/ml-platform/customTask/detail?Id=${job.jobId}"
    outputPath.write(s"Task [${job.jobId}] final state: $status, for detailed task info, you can " +
      s"visit: $volcCustomTaskSite")(Seq(StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.SYNC))
    ()
  }

}

case class VolcTaskSubmitResponse(
                                   rc: Int,
                                   jobId: String,
                                   failedCause: Option[Throwable] = None
                                 )

case class VolcTaskGetResponse(
                                rc: Int,
                                volcTaskInfo: Option[VolcTaskInfo]
                              )

case class VolcTaskInfo(
                         taskName: String,
                         taskId: String,
                         image: String,
                         createTime: String,
                         launchTime: String,
                         state: VolcTaskRunState,
                         entrypointPath: String,
                         args: Seq[String],
                         framework: String,
                         exitCode: Int,
                         roleSpecs: Seq[TaskRoleSpec]
                       )

case class VolcTaskCancelResponse(
                                   rc: Int
                                 )

case class TaskConfigYaml(
                           TaskName: String,
                           Description: String,
                           Entrypoint: String,
                           Tags: Option[Seq[String]],
                           Envs: Option[Seq[String]],
                           Image: String,
                           ImageUrl: Option[String],
                           ImageCredential: Option[ImageCredentialYaml],
                           SidecarImage: Option[String],
                           ResourceQueueID: String,
                           Framework: String,
                           TaskRoleSpecs: Seq[TaskRoleSpecYaml],
                           ActiveDeadlineSeconds: Int,
                           EnableTensorboard: Boolean,
                           SidecarMemoryRatio: Double,
                           Storages: Seq[VolcStorageYaml]
                         )

case class TaskRoleSpecYaml(
                             RoleName: String,
                             RoleReplicas: Int,
                             Flavor: String
                           )


case class ImageCredentialYaml(
                                RegistryUsername: String,
                                RegistryToken: String
                              )

object TaskConfigYamlJsonSupport extends DefaultJsonProtocol {
  implicit val imageCredentialFormat: RootJsonFormat[ImageCredentialYaml] = jsonFormat2(ImageCredentialYaml)
  implicit val taskRoleSpecFormat: RootJsonFormat[TaskRoleSpecYaml] = jsonFormat3(TaskRoleSpecYaml)
  implicit val volcStorageFormat: RootJsonFormat[VolcStorageYaml] = jsonFormat5(VolcStorageYaml)
  implicit val taskConfigYamlFormat: RootJsonFormat[TaskConfigYaml] = jsonFormat16(TaskConfigYaml)
}

object LocalTest {
  def main(args: Array[String]): Unit = {

    import TaskConfigYamlJsonSupport._
    val y = TaskConfigYaml(
      "aa",
      "ssd",
      "",
      Some(Seq("a", "d")),
      Some(Seq.empty),
      "ima",
      Some(""),
      None,
      Some(""),
      "que",
      "Custom",
      Seq(TaskRoleSpecYaml("worker", 2, "ss")),
      22,
      EnableTensorboard = false,
      0.5,
      Seq()
      //      TaskRoleSpec("worker", 33, "ddf")
    )
    val ys = y.toJson.prettyPrint
    println(ys)
    import cats.syntax.either._
    //    import io.circe.yaml._
    import io.circe.yaml.syntax._
    println(io.circe.jawn.parse(ys).valueOr(throw _).asYaml.spaces2)

    val yamlstr = io.circe.jawn.parse(ys) match {
      case Left(value) => value
      case Right(value) =>
        io.circe.yaml
          .Printer(dropNullKeys = true, indent = 2)
          .pretty(value)
          .linesIterator
          .map(line => if (line.startsWith("-") || line.startsWith(" ")) "  " + line else line)
          .mkString("\n")

    }

    println(yamlstr)

    val submitStr =
      """
        |volc当前版本:1.2.3,最新版本:1.2.5(变更日志:https://www.volcengine.com/docs/6459/80261)
        |升级方法:
        |1.volc upgrade
        |2.sh -c "$(curl -fsSL https://ml-platform-public-examples-cn-beijing.tos-cn-beijing.volces
        |.com/cli-binary/install.sh)"
        |
        |EnableTensorboard is not supported in current version, please upgrade volc to latest version. See changelog
        |for detail: https://www.volcengine.com/docs/6459/80261
        |创建任务成功,task_id=t-20220611172423-f49fk
        |
        |
        |""".stripMargin
    val reg = "创建任务成功,task_id=[a-z0-9-]+".r
    val r = reg.findFirstIn(submitStr) match {
      case None => ""
      case Some(s) => s.trim.substring(15)
    }

    //    val submitStr = volcTask.volcJobPaths.volcSubmitStdout.contentAsString(Charsets.UTF_8)
    //    val reg = "创建任务成功,task_id=[a-z0-9-]+".r
    //    reg.findFirstIn(submitStr) match {
    //      case None => ""
    //      case Some(s) => s.trim.substring(15)
    //    }

    println(r)
  }
}