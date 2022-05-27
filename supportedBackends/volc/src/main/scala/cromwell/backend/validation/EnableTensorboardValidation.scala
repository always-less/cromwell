package cromwell.backend.validation

import cromwell.backend.impl.volc.{VolcRuntimeAttributeKeys}


object EnableTensorboardValidation {
  def optional(): OptionalRuntimeAttributesValidation[Boolean] = new EnableTensorboardValidation().optional
}

class EnableTensorboardValidation extends BooleanRuntimeAttributesValidation(VolcRuntimeAttributeKeys.enableTensorboard)
