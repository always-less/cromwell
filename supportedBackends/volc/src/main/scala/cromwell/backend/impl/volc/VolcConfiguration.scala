package cromwell.backend.impl.volc

import com.typesafe.config.Config
import cromwell.backend.BackendConfigurationDescriptor
import cromwell.backend.impl.volc.VolcConfiguration.tosConfig
import net.ceedubs.ficus.Ficus._

class VolcConfiguration(val configurationDescriptor: BackendConfigurationDescriptor) {

  val runtimeConfig: Option[Config] = configurationDescriptor.backendRuntimeAttributesConfig
  val queue: Option[String] = configurationDescriptor.backendConfig.as[Option[String]]("queue")
  val tos: TosConfig = TosConfig(configurationDescriptor.backendConfig.as[Config](tosConfig))
  val executionRootDir: String = configurationDescriptor.backendConfig.getOrElse(VolcConfiguration.executionRootDir, "cromwell-executions")
}

object VolcConfiguration {
  final val queue = "queue"
  final val tosConfig = "tos"
  final val tosBucket = "bucket"
  final val tosPrefix = "prefix"
  final val tosMountPath = "mount-path"
  final val tosAK = "ak"
  final val tosSK = "sk"
  final val executionRootDir = "execution-root-dir"
}
