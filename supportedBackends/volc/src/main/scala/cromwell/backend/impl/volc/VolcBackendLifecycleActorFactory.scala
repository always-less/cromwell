package cromwell.backend.impl.volc

import akka.actor.ActorRef
import cromwell.backend._
import cromwell.backend.standard._
import wom.graph.CommandCallNode

case class VolcBackendLifecycleActorFactory(name: String, configurationDescriptor: BackendConfigurationDescriptor)
  extends StandardLifecycleActorFactory {
  override lazy val initializationActorClass: Class[_ <: StandardInitializationActor] = classOf[VolcInitializationActor]
  override lazy val asyncExecutionActorClass: Class[_ <: StandardAsyncExecutionActor] =
    classOf[VolcAsyncBackendJobExecutionActor]

  override def jobIdKey: String = VolcAsyncBackendJobExecutionActor.JobIdKey

  val volcConfiguration = new VolcConfiguration(configurationDescriptor)

  override def workflowInitializationActorParams(workflowDescriptor: BackendWorkflowDescriptor, ioActor: ActorRef, calls: Set[CommandCallNode],
                                                 serviceRegistryActor: ActorRef, restarting: Boolean): StandardInitializationActorParams = {
    VolcInitializationActorParams(workflowDescriptor, calls, volcConfiguration, serviceRegistryActor)
  }
}
