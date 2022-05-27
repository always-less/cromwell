package cromwell.backend.impl.volc

import akka.actor.Actor
import cromwell.backend.standard.StandardCachingActorHelper
import cromwell.core.logging.JobLogging

trait VolcJobCachingActorHelper extends StandardCachingActorHelper {
  this: Actor with JobLogging =>

  lazy val initializationData: VolcBackendInitializationData = {
    backendInitializationDataAs[VolcBackendInitializationData]
  }

  lazy val volcWorkflowPaths: VolcWorkflowPaths = workflowPaths.asInstanceOf[VolcWorkflowPaths]

  lazy val volcJobPaths: VolcJobPaths = jobPaths.asInstanceOf[VolcJobPaths]

  lazy val volcConfiguration: VolcConfiguration = initializationData.tesConfiguration

  lazy val runtimeAttributes: VolcRuntimeAttributes = VolcRuntimeAttributes(validatedRuntimeAttributes, jobDescriptor.runtimeAttributes, volcConfiguration)

}
