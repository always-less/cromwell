package cromwell.backend.impl.volc

import cromwell.backend.{BackendConfigurationDescriptor, BackendJobDescriptor}
import cromwell.core.logging.JobLogger
import cromwell.core.path.Path
import wom.InstantiatedCommand

//import scala.language.postfixOps

final case class VolcTask(
    volcConfiguration: VolcConfiguration,
    jobDescriptor: BackendJobDescriptor,
    configurationDescriptor: BackendConfigurationDescriptor,
    jobLogger: JobLogger,
    volcJobPaths: VolcJobPaths,
    runtimeAttributes: VolcRuntimeAttributes,
    taskWorkDir: Path,
    instantiatedCommand: InstantiatedCommand
) {

//  private val workflowDescriptor = jobDescriptor.workflowDescriptor
//  private val workflowName = workflowDescriptor.callable.name
  private val fullyQualifiedTaskName = jobDescriptor.taskCall.fullyQualifiedName
  val name: String = fullyQualifiedTaskName
  val description: String = jobDescriptor.toString

  def writeTaskConfigYamlFile(): Unit = {
    jobLogger.info("-----, writeTaskConfigYamlFile")
  }
}

object VolcTask {}
