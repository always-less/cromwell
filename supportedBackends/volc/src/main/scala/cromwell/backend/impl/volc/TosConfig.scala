package cromwell.backend.impl.volc

import com.typesafe.config.Config
import cromwell.backend.validation.SupportStorage.{TosStorageType}

object TosConfig {
  def apply(tosConfig: Config): TosConfig = {
    TosConfig(
      tosConfig.getString(VolcConfiguration.tosBucket),
      tosConfig.getString(VolcConfiguration.tosPrefix),
      tosConfig.getString(VolcConfiguration.tosMountPath),
      tosConfig.getString(VolcConfiguration.tosAK),
      tosConfig.getString(VolcConfiguration.tosSK)
    )
  }
}

case class TosConfig
(
  bucket: String,
  prefix: String,
  mountPath: String,
  ak: String,
  sk: String
) {
  lazy val volcTosStorage: TosStorageType = TosStorageType(
    "Tos",
    mountPath,
    bucket,
    prefix
  )
}