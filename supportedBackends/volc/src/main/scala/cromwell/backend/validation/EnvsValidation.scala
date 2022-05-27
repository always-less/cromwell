package cromwell.backend.validation

import cats.implicits.catsSyntaxValidatedId
import common.validation.ErrorOr.ErrorOr
import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys
import wom.types.{WomArrayType, WomStringType, WomType}
import wom.values.WomValue


object EnvsValidation {
  def optional(): OptionalRuntimeAttributesValidation[Seq[String]] = new EnvsValidation().optional
}

class EnvsValidation extends RuntimeAttributesValidation[Seq[String]] {
  override def key: String = VolcRuntimeAttributeKeys.envs

  override def coercion: Traversable[WomType] = Set(WomArrayType(WomStringType))

  override protected def validateValue: PartialFunction[WomValue, ErrorOr[Seq[String]]] = {
    case _ => Seq.empty.validNel
  }
}
