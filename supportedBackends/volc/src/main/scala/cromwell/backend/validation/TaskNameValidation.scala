package cromwell.backend.validation

import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys

object TaskNameValidation {
  def optional(): OptionalRuntimeAttributesValidation[String] = new TaskNameValidation().optional
}

class TaskNameValidation extends StringRuntimeAttributesValidation(VolcRuntimeAttributeKeys.taskName)

