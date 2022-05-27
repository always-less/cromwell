package cromwell.backend.impl.volc

import cromwell.backend.standard.{StandardExpressionFunctions, StandardInitializationData, StandardValidatedRuntimeAttributesBuilder}

case class VolcBackendInitializationData
(
  override val workflowPaths: VolcWorkflowPaths,
  override val runtimeAttributesBuilder: StandardValidatedRuntimeAttributesBuilder,
  tesConfiguration: VolcConfiguration
) extends StandardInitializationData(workflowPaths, runtimeAttributesBuilder, classOf[StandardExpressionFunctions])
