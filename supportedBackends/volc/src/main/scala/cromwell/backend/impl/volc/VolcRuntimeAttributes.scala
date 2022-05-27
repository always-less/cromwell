package cromwell.backend.impl.volc

import com.typesafe.config.Config
import cromwell.backend.standard.StandardValidatedRuntimeAttributesBuilder
import cromwell.backend.validation.SupportStorage.{VolcStorageType}
import cromwell.backend.validation._
import eu.timepit.refined.api.Refined
import wom.values._

case class VolcRuntimeAttributes(
    continueOnReturnCode: ContinueOnReturnCode,
    taskName: Option[String],
    description: Option[String],
    tags: Option[Seq[String]],
    envs: Option[Seq[String]],
    image: Option[String],
    imageUrl: Option[String],
    imageCredential: Option[ImageCredential],
    sidecarImage: Option[String],
    resourceQueueId: Option[String],
    framework: Option[String Refined FrameworkCollections],
    taskRoleSpecs: Option[Seq[WomValue] Refined TaskRoleSpecType],
    activeDeadlineSeconds: Option[Int],
    flavor: Option[String], // todo
    enableTensorboard: Option[Boolean],
    sidecarMemoryRatio: Option[Double],
    storages: Option[Seq[WomValue] Refined VolcStorageType]
)


case class ImageCredential(
    registryUsername: String,
    registryToken: String
) {
  def toYaml: ImageCredentialYaml = {
    ImageCredentialYaml(
      registryUsername,
      registryToken
    )
  }
}

//object VolcStorage {
//  def apply(womArray: WomArray): Seq[VolcStorage] = {
//    Seq.empty // todo
//  }
//}



object VolcRuntimeAttributes {

  private def taskNameValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    TaskNameValidation.optional()

  private def descriptionValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    DescriptionValidation.optional()

  private def tagsValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Seq[String]] =
    TagsValidation.optional()

  private def envsValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Seq[String]] =
    EnvsValidation.optional()

  private def imageValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    ImageValidation.optional()

  private def imageUrlValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    ImageUrlValidation.optional()

  private def imageCredentialValidation(runtimeConfig: Option[Config])
  : OptionalRuntimeAttributesValidation[ImageCredential] =
    ImageCredentialValidation.optional()

  private def sidecarImageValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    SidecarImageValidation.optional()

  private def resourceQueueIdValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    ResourceQueueIdValidation.optional()

  private def frameworkValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Refined[String,
    FrameworkCollections]] =
    FrameworkValidation.optional


  private def taskRoleSpecsValidation(runtimeConfig: Option[Config])
  : OptionalRuntimeAttributesValidation[Seq[WomValue] Refined TaskRoleSpecType] =
    TaskRoleSpecsValidation.optional

  private def activeDeadlineSecondsValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Int] =
    ActiveDeadlineSecondsValidation.optional()

  private def flavorValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[String] =
    FlavorValidation.optional()

  private def enableTensorboardValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Boolean] =
    EnableTensorboardValidation.optional()

  private def sidecarMemoryRatioValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Double] =
    SidecarMemoryRatioValidation.optional()

  private def storagesValidation(runtimeConfig: Option[Config]): OptionalRuntimeAttributesValidation[Seq[WomValue]
    Refined VolcStorageType] = StoragesValidation.optional

  private def continueOnReturnCodeValidation(runtimeConfig: Option[Config]) = ContinueOnReturnCodeValidation.default(runtimeConfig)

  def runtimeAttributesBuilder(backendRuntimeConfig: Option[Config]): StandardValidatedRuntimeAttributesBuilder =
    StandardValidatedRuntimeAttributesBuilder
      .default(backendRuntimeConfig)
      .withValidation(
        continueOnReturnCodeValidation(backendRuntimeConfig),
        taskNameValidation(backendRuntimeConfig),
        descriptionValidation(backendRuntimeConfig),
        tagsValidation(backendRuntimeConfig),
        envsValidation(backendRuntimeConfig),
        imageValidation(backendRuntimeConfig),
        imageUrlValidation(backendRuntimeConfig),
        imageCredentialValidation(backendRuntimeConfig),
        sidecarImageValidation(backendRuntimeConfig),
        resourceQueueIdValidation(backendRuntimeConfig),
        frameworkValidation(backendRuntimeConfig),
        taskRoleSpecsValidation(backendRuntimeConfig),
        activeDeadlineSecondsValidation(backendRuntimeConfig),
        flavorValidation(backendRuntimeConfig),
        enableTensorboardValidation(backendRuntimeConfig),
        sidecarMemoryRatioValidation(backendRuntimeConfig),
        storagesValidation(backendRuntimeConfig)
      )

  def apply(validatedRuntimeAttributes: ValidatedRuntimeAttributes,
            rawRuntimeAttributes: Map[String, WomValue],
            config: VolcConfiguration
           ): VolcRuntimeAttributes = {
    val backendRuntimeConfig = config.runtimeConfig
    new VolcRuntimeAttributes(
      RuntimeAttributesValidation.extract(continueOnReturnCodeValidation(backendRuntimeConfig),
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(taskNameValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(descriptionValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(tagsValidation(backendRuntimeConfig).key, validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(envsValidation(backendRuntimeConfig).key, validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(imageValidation(backendRuntimeConfig).key, validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(imageUrlValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(imageCredentialValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(sidecarImageValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(resourceQueueIdValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(frameworkValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(taskRoleSpecsValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(activeDeadlineSecondsValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(flavorValidation(backendRuntimeConfig).key, validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(enableTensorboardValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(sidecarMemoryRatioValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes),
      RuntimeAttributesValidation.extractOption(storagesValidation(backendRuntimeConfig).key,
        validatedRuntimeAttributes)
    )
  }
}
