package cromwell.backend.impl.volc.util

import cromwell.backend.impl.volc.TaskConfigYaml
import io.circe.ParsingFailure
import spray.json._

object YamlUtil {

  def toYamlString(taskConfigYaml: TaskConfigYaml): Either[ParsingFailure, String] = {
    import cromwell.backend.impl.volc.TaskConfigYamlJsonSupport._

    val configJson = taskConfigYaml.toJson.prettyPrint
    io.circe.jawn.parse(configJson) map {
      json => io.circe.yaml.Printer(maxScalarWidth = 500).pretty(json)
    }
  }
}
