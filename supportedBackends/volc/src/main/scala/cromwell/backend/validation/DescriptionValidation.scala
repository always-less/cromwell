package cromwell.backend.validation

import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys

object DescriptionValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new DescriptionValidation().optional
}

class DescriptionValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.description)