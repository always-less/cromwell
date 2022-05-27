package cromwell.backend.validation

import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys

object ImageUrlValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new ImageUrlValidation().optional
}

class ImageUrlValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.imageUrl)
