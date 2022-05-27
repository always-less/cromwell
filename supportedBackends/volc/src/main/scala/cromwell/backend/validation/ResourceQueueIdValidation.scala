package cromwell.backend.validation

import cromwell.backend.impl.volc.{VolcRuntimeAttributeKeys}

object ResourceQueueIdValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new ResourceQueueIdValidation().optional
}

class ResourceQueueIdValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.resourceQueueId)
