package cromwell.backend.validation

import cromwell.backend.impl.volc.{VolcRuntimeAttributeKeys}

object ActiveDeadlineSecondsValidation {
  def optional(): OptionalRuntimeAttributesValidation[Int] = new ActiveDeadlineSecondsValidation().optional
}

class ActiveDeadlineSecondsValidation extends IntRuntimeAttributesValidation(VolcRuntimeAttributeKeys.activeDeadlineSeconds)
