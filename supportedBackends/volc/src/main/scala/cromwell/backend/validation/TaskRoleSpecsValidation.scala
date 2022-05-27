package cromwell.backend.validation


import cats.syntax.either._
import cats.data.NonEmptyList
import common.validation.ErrorOr.ErrorOr
import cromwell.backend.impl.volc.{TaskRoleSpecYaml, VolcRuntimeAttributeKeys}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import wom.types.{WomArrayType, WomMapType, WomStringType, WomType}
import wom.values.{WomArray, WomMap, WomString, WomValue}

object TaskRoleSpecsValidation {

  //  def getDefaultTaskRoleSpec(flavor: String): Seq[WomMap] Refined TaskRoleSpecType = Refined
  //  .unsafeApply[Seq[WomMap],
  //    TaskRoleSpecType](Seq(WomMap(Map("roleName" -> "worker", "roleReplicas" ->, "1", "flavor" -> s"${flavor}"))))

  lazy val instance: RuntimeAttributesValidation[Seq[WomValue] Refined TaskRoleSpecType] = new TaskRoleSpecsValidation()

  lazy val optional: OptionalRuntimeAttributesValidation[Seq[WomValue] Refined TaskRoleSpecType] = instance.optional


  def constructTaskRoleSpecs(storage: Option[Seq[WomValue] Refined TaskRoleSpecType]): Option[Seq[TaskRoleSpec]] = {
    storage match {
      case a@Some(_) =>
        Some(a.get.value.map {
          case WomMap(_, mapValue) =>
            TaskRoleSpec(
              mapValue(WomString("RoleName")).valueString,
              mapValue(WomString("RoleReplicas")).valueString.toInt,
              mapValue(WomString("Flavor")).valueString,
            )
        })
      case None => None
    }
  }

}

case class TaskRoleSpec(
                         roleName: String,
                         roleReplicas: Int,
                         flavor: String
                       ) {
  def toYaml: TaskRoleSpecYaml = {
    TaskRoleSpecYaml(
      roleName,
      roleReplicas,
      flavor
    )
  }
}

class TaskRoleSpecsValidation extends RuntimeAttributesValidation[Seq[WomValue] Refined TaskRoleSpecType] {
  override def key: String = VolcRuntimeAttributeKeys.taskRoleSpecs

  override def coercion: Traversable[WomType] = Seq[WomArrayType](WomArrayType(WomMapType(WomStringType,
    WomStringType)))


  override protected def validateValue: PartialFunction[WomValue, ErrorOr[Seq[WomValue] Refined TaskRoleSpecType]] = {
    case arr: WomArray => {
      refineV[TaskRoleSpecType](arr.value)
        .leftMap(re => NonEmptyList.one(s"${re}"))
        .toValidated
    }
  }
}

case class TaskRoleSpecType()


object TaskRoleSpecType {

  val TaskRoleSpecKey = Set("RoleName", "RoleReplicas", "Flavor")
  val RoleReplicasNotIntegerErr = "please check the value of RoleReplicas,has to be positive Interger"
  val TaskRoleSpecsKeyErr = s"please check the key of TaskRoleSpec, the key expect to have ${TaskRoleSpecKey}"
  val KeyERR = 0
  val ReplicaERR = 1
  var errCode = 0

  implicit def taskRoleSpecValidate: Validate.Plain[Seq[WomValue], TaskRoleSpecType] =
    Validate.fromPredicate(a => checkKey(a) && checkVal(a), t => s"TaskRoleSpecs config has error,${
      errCode match {
        case KeyERR => TaskRoleSpecsKeyErr
        case ReplicaERR => RoleReplicasNotIntegerErr
      }
    }\ncurrent is ${t}",
      TaskRoleSpecType())

  def checkKey(taskRoleSeqMap: Seq[WomValue]): Boolean = {
    taskRoleSeqMap.map {
      case WomMap(_, matchValue) => (matchValue.keySet.map(a => a.valueString)) == TaskRoleSpecKey
      case _ => false
    }.collectFirst {
      case false => errCode = KeyERR; false;
    }.getOrElse(true)
  }

  def checkVal(taskRoleSeqMap: Seq[WomValue]): Boolean = {
    taskRoleSeqMap.map {
      case WomMap(_, matchValue) => {
        val str = matchValue.getOrElse(WomString("RoleReplicas"), WomString("empty")).valueString
        str.forall(Character.isDigit) && str.toInt > 0
      }
      case _ => false
    }.collectFirst {
      case false => errCode = ReplicaERR; false;
    }.getOrElse(true)
  }


}
