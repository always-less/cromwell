package cromwell.backend.validation

import cats.implicits.catsSyntaxValidatedId
import common.validation.ErrorOr.ErrorOr
import cromwell.backend.impl.volc.{ImageCredential, VolcRuntimeAttributeKeys}
import wom.types.{WomObjectType, WomType}
import wom.values.WomValue


object ImageCredentialValidation {
  def optional(): OptionalRuntimeAttributesValidation[ImageCredential] = new ImageCredentialValidation().optional
}

class ImageCredentialValidation extends RuntimeAttributesValidation[ImageCredential] {
  override def key: String = VolcRuntimeAttributeKeys.imageCredential

  override def coercion: Traversable[WomType] = Set(WomObjectType)

  override protected def validateValue: PartialFunction[WomValue, ErrorOr[ImageCredential]] = {
    case _ => ImageCredential("", "").validNel
  }
}


