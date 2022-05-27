package cromwell.backend.impl.volc

import akka.actor.ActorRef
import cromwell.backend.standard._
import cromwell.backend.{BackendConfigurationDescriptor, BackendInitializationData, BackendWorkflowDescriptor}
import wom.graph.CommandCallNode

import scala.concurrent.Future

case class VolcInitializationActorParams
(
  workflowDescriptor: BackendWorkflowDescriptor,
  calls: Set[CommandCallNode],
  volcConfiguration: VolcConfiguration,
  serviceRegistryActor: ActorRef
) extends StandardInitializationActorParams {
  override val configurationDescriptor: BackendConfigurationDescriptor = volcConfiguration.configurationDescriptor
}

class VolcInitializationActor(params: VolcInitializationActorParams)
  extends StandardInitializationActor(params) {

  private val volcConfiguration = params.volcConfiguration
  override lazy val workflowPaths: Future[VolcWorkflowPaths] = pathBuilders map {
    VolcWorkflowPaths(volcConfiguration, workflowDescriptor, volcConfiguration.configurationDescriptor.backendConfig, _)
  }

  // todo
  override lazy val runtimeAttributesBuilder: StandardValidatedRuntimeAttributesBuilder =
    VolcRuntimeAttributes.runtimeAttributesBuilder(volcConfiguration.runtimeConfig)

  override def beforeAll(): Future[Option[BackendInitializationData]] = {
    workflowPaths map { paths =>
      publishWorkflowRoot(paths.workflowRoot.toString)
      paths.workflowRoot.createPermissionedDirectories()
      Option(VolcBackendInitializationData(paths, runtimeAttributesBuilder, volcConfiguration))
    }
  }
}
