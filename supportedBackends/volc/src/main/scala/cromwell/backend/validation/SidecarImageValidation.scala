package cromwell.backend.validation

import cromwell.backend.impl.volc.{VolcRuntimeAttributeKeys}


object SidecarImageValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new SidecarImageValidation().optional
}

class SidecarImageValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.sidecarImage)
