package cromwell.backend.validation

import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys

object FlavorValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new FlavorValidation().optional
}

class FlavorValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.flavor)
