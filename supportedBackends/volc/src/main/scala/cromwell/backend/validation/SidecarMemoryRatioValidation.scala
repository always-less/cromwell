package cromwell.backend.validation

import cromwell.backend.impl.volc.{VolcRuntimeAttributeKeys}

object SidecarMemoryRatioValidation {
  def optional(): OptionalRuntimeAttributesValidation[Double] = new SidecarMemoryRatioValidation().optional
}

class SidecarMemoryRatioValidation extends FloatRuntimeAttributesValidation(VolcRuntimeAttributeKeys.sidecarMemoryRatio)
