package cromwell.backend.validation

import cats.syntax.either._
import cats.data.NonEmptyList
import common.validation.ErrorOr.ErrorOr
import cromwell.backend.impl.volc.VolcRuntimeAttributeKeys
import eu.timepit.refined.api.{Refined, Validate}
import wom.types.WomStringType
import wom.values.{WomString, WomValue}
import cromwell.backend.validation.SupportStorage.{NasStorageType, TosStorageType, VepFSStorageType, VolcStorageType, VolcStorageYamlTrait}
import eu.timepit.refined.refineV
import wom.types.{WomArrayType, WomMapType, WomType}
import wom.values.{WomArray, WomMap}



object StoragesValidation {
  //  val default: Seq[WomMap] Refined TosStorageType = Refined.unsafeApply[Seq[WomMap], TosStorageType](Seq(WomMap
  //  (Map())))

  lazy val instance: RuntimeAttributesValidation[Seq[WomValue] Refined VolcStorageType] = new StoragesValidation()

  lazy val optional: OptionalRuntimeAttributesValidation[Seq[WomValue] Refined VolcStorageType] = instance.optional

  def constructVolcStorage(storage: Option[Seq[WomValue] Refined VolcStorageType]): Option[Seq[VolcStorageYamlTrait]]
  = {
    storage match {
      case a@Some(_) =>
        Some(a.get.value.map {
          case WomMap(_, matchValue) => matchValue.getOrElse(WomString("type"), WomString("invalid")) match {
            case WomString("Tos") => TosStorageType(
              matchValue(WomString("type")).valueString,
              matchValue(WomString("mount_path")).valueString,
              matchValue(WomString("bucket")).valueString,
              matchValue(WomString("prefix")).valueString)

            case WomString("Nas") => NasStorageType(
              matchValue(WomString("type")).valueString,
              matchValue(WomString("mount_path")).valueString,
              matchValue(WomString("nas_addr")).valueString,
            )
            case WomString("Vepfs") => VepFSStorageType(
              matchValue(WomString("type")).valueString,
              matchValue(WomString("mount_path")).valueString,
            )
          }
        })
      case None => None
    }
  }
}

class StoragesValidation extends RuntimeAttributesValidation[Seq[WomValue] Refined VolcStorageType] {
  override def key: String = VolcRuntimeAttributeKeys.storages

  override def coercion: Traversable[WomType] = Set[WomArrayType](WomArrayType(WomMapType(WomStringType,
    WomStringType)))

  override protected def validateValue: PartialFunction[WomValue, ErrorOr[Seq[WomValue] Refined VolcStorageType]] = {
    case arr: WomArray => {
      refineV[VolcStorageType](arr.value)
        .leftMap(re => NonEmptyList.one(s"${re}"))
        .toValidated
    }
  }
}


object SupportStorage {

  case class VolcStorageYaml(Type: String, MountPath: String, Bucket: Option[String] = None, Prefix: Option[String] =
  None, NasAddr: Option[String] = None)


  trait VolcStorageYamlTrait {
    def toYaml: VolcStorageYaml
  }

  case class VolcStorageType() extends VolcStorageYamlTrait {
    override def toYaml: VolcStorageYaml = VolcStorageYaml("base", "base")
  }

  case class TosStorageType(`type`: String, mountPath: String, bucket: String, prefix: String) extends
    VolcStorageYamlTrait {
    override def toYaml: VolcStorageYaml = VolcStorageYaml(
      Type = `type`,
      MountPath = mountPath,
      Bucket = Some(bucket),
      Prefix = Some(prefix))
  }

  case class NasStorageType(`type`: String, MountPath: String, NasAddr: String) extends
    VolcStorageYamlTrait {
    override def toYaml: VolcStorageYaml = VolcStorageYaml(
      Type = `type`,
      MountPath = MountPath,
      NasAddr = Some(NasAddr))
  }

  case class VepFSStorageType(`type`: String, MountPath: String) extends VolcStorageYamlTrait {
    override def toYaml: VolcStorageYaml = VolcStorageYaml(Type = `type`, MountPath = MountPath)
  }


  trait VolcStorageValidation {

    def validateType(storageItem: Map[WomValue, WomValue]): (Boolean, String)
  }

  object VolcStorageType {
    implicit def tosStorageValidate: Validate.Plain[Seq[WomValue], VolcStorageType] =
      Validate.fromPredicate(a => validateType(a)._1, t => s"storage config has error: \n${validateType(t)._2}", VolcStorageType())

    def validateType(storageSeqMap: Seq[WomValue]): (Boolean, String) = {
      val errorMsg = storageSeqMap.map {
        case WomMap(_, matchValue) =>
          matchValue.get(WomString("type")) match {
            case Some(WomString("Tos")) => TosStorageType.validateType(matchValue)
            case Some(WomString("Nas")) => NasStorageType.validateType(matchValue)
            case Some(WomString("Vepfs")) => VepFSStorageType.validateType(matchValue)
            case Some(WomString(x)) => (false,s"invalid type value: ${x}")
            case Some(v) => (false,s"invalid type TYPE: ${v}")
            case None => (false,s"Field type is required")
          }
        case _ => (false, "invalid map structure")
      }.filter(a => !a.asInstanceOf[Tuple2[Boolean, String]]._1).
        map(a => a.asInstanceOf[Tuple2[Boolean, String]]._2).mkString("\n")
      (errorMsg.isBlank, errorMsg)
    }
  }


  object TosStorageType extends VolcStorageValidation {
    val TosMapKey = Set("type", "mount_path", "bucket", "prefix")
    val KeyErrMsg = "please check the following key of Tos Storage: "


    override def validateType(storageItem: Map[WomValue, WomValue]): (Boolean,String) = validateKey(storageItem)

    def validateKey(storageSeqMap: Map[WomValue, WomValue]): (Boolean,String) = {
      val errorMsg=TosMapKey.filter(k => !storageSeqMap.contains(WomString(k))).mkString(",")
      (errorMsg.isBlank,s"${KeyErrMsg} ${errorMsg}")
    }
  }

  object NasStorageType extends VolcStorageValidation {
    val NasMapKey = Set("type", "mount_path", "nas_addr")
    val KeyErrMsg = "please check the following key of Nas Storage: "
    override def validateType(storageItem: Map[WomValue, WomValue]) = validateKey(storageItem)

    def validateKey(storageSeqMap: Map[WomValue, WomValue]): (Boolean,String) = {
      val errorMsg=NasMapKey.filter(k => !storageSeqMap.contains(WomString(k))).mkString(",")
      (errorMsg.isBlank,s"${KeyErrMsg} ${errorMsg}")
    }
  }

  object VepFSStorageType extends VolcStorageValidation {
    val VepFSMapKey = Set("type", "mount_path")
    val KeyErrMsg = "please check the following key of VepFS Storage: "
    override def validateType(storageItem: Map[WomValue, WomValue]) = validateKey(storageItem)

    def validateKey(storageSeqMap: Map[WomValue, WomValue]): (Boolean,String) = {
      val errorMsg=VepFSMapKey.filter(k => !storageSeqMap.contains(WomString(k))).mkString(",")
      (errorMsg.isBlank,s"${KeyErrMsg} ${errorMsg}")
    }
  }

}


//object StorageMap {
//
//  val AvailableType = Set("Tos")
//
//
//}

