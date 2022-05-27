package cromwell.backend.validation

import cats.syntax.either._
import cats.data.NonEmptyList
import common.validation.ErrorOr.ErrorOr
import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys
import cromwell.backend.validation.CpuValidation.defaultMin.womType
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}
import wom.types.WomStringType
import wom.values.{WomString, WomValue}

object FrameworkValidation {
  val default: String Refined FrameworkCollections = Refined.unsafeApply[String, FrameworkCollections]("Custom")
  lazy val instance: RuntimeAttributesValidation[String Refined FrameworkCollections] = new FrameworkValidation(VolcRuntimeAttributeKeys.framework)
  lazy val optional: OptionalRuntimeAttributesValidation[String Refined FrameworkCollections] = instance.optional
}

class FrameworkValidation(override val key: String) extends RuntimeAttributesValidation[String Refined
  FrameworkCollections] {

  override def coercion = Seq(WomStringType)

  override protected def validateExpression: PartialFunction[WomValue, Boolean] = {
    case womValue if WomStringType.coerceRawValue(womValue).isSuccess => true
  }

  override protected def missingValueMessage: String =
    s"expecting $key  to be one of ${FrameworkCollections.FrameworkCollection}"

  protected def typeString = s"a ${womType.stableName}"

  override protected def validateValue: PartialFunction[WomValue, ErrorOr[String Refined FrameworkCollections]] = {
    case womValue if WomStringType.coerceRawValue(womValue).isSuccess =>
      WomStringType.coerceRawValue(womValue).get match {
        case WomString(value) => {
          refineV[FrameworkCollections](value.toString)
            .leftMap(re => NonEmptyList.one(s"$re"))
            .toValidated
        }
      }
  }

}

case class FrameworkCollections()

object FrameworkCollections {
  val FrameworkCollection = Set("TensorFlowPS","PyTorchDDP","MXNet","Custom","BytePS","Horovod","MPI","Slurm")

  implicit def frameworkValidate: Validate.Plain[String, FrameworkCollections] =
    Validate.fromPredicate(a => FrameworkCollection.contains(a), t => s"Framework is ('$t') it needs to be " +
      s"${FrameworkCollection}", FrameworkCollections())
}
//
//case class TensorFlowPS() extends FrameworkCollections
//
//case class PytorchDDP() extends FrameworkCollections
//
//case class MXNet() extends FrameworkCollections
//
//case class BytePS() extends FrameworkCollections
//
//case class MPI() extends FrameworkCollections
//
//case class Custom() extends FrameworkCollections