package cromwell.backend.validation

import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys

object ImageValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new ImageValidation().optional
}

class ImageValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.image)
