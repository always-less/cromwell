package cromwell.backend.impl.volc

import cromwell.backend.BackendJobDescriptorKey
import cromwell.backend.io.JobPaths
import cromwell.core.path._

case class VolcJobPaths private[volc](override val workflowPaths: VolcWorkflowPaths,
                                      jobKey: BackendJobDescriptorKey,
                                      override val isCallCacheCopyAttempt: Boolean = false) extends JobPaths {

  override lazy val callExecutionRoot: Path = {
    callRoot.resolve("execution")
  }

  lazy val volcSubmitStdout: Path = callExecutionRoot.resolve("volc-submit-stdout")
  lazy val volcSubmitStderr: Path = callExecutionRoot.resolve("volc-submit-stderr")
  lazy val volcGetStdout: Path = callExecutionRoot.resolve("volc-get-stdout")
  lazy val volcGetStderr: Path = callExecutionRoot.resolve("volc-get-stderr")
  lazy val volcCancelStdout: Path = callExecutionRoot.resolve("volc-cancel-stdout")
  lazy val volcCancelStderr: Path = callExecutionRoot.resolve("volc-cancel-stderr")

  // Given an output path, return a path localized to the storage file system
  def storageOutput(path: String): String = {
    callExecutionRoot.resolve(path).toString
  }

  override def forCallCacheCopyAttempts: JobPaths = this.copy(isCallCacheCopyAttempt = true)
}
