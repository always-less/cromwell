package cromwell.filesystems.oss.nio

import java.net.URI
import com.typesafe.config.{Config, ConfigFactory}
import cromwell.core.TestKitSuite
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object TTLOssStorageConfigurationSpec {

  val BcsBackendConfigString: String =
    s"""
     |  auth {
     |      endpoint = "oss-cn-shanghai.aliyuncs.com"
     |      access-id = "test-access-id"
     |      access-key = "test-access-key"
     |      security-token = "test-security-token"
     |  }
     |  caching {
     |      duplication-strategy = "reference"
     |  }
      """.stripMargin

  val BcsBackendConfig: Config = ConfigFactory.parseString(BcsBackendConfigString)
}

class TTLOssStorageConfigurationSpec extends TestKitSuite with AnyFlatSpecLike with Matchers with BeforeAndAfter {
  val expectedEndpoint = "oss-cn-shanghai.aliyuncs.com"
  val expectedAccessId = "test-access-id"
  val expectedAccessKey = "test-access-key"
  val expectedToken: Option[String] = Option("test-security-token")
  val expectedFullEndpoint: URI = URI.create("http://oss-cn-shanghai.aliyuncs.com")

  behavior of "TTLOssStorageConfiguration"


  it should "have correct OSS credential info" in {

    val ossConfig = TTLOssStorageConfiguration(TTLOssStorageConfigurationSpec.BcsBackendConfig)

    ossConfig.endpoint shouldEqual expectedEndpoint
    ossConfig.accessId shouldEqual expectedAccessId
    ossConfig.accessKey shouldEqual expectedAccessKey
    ossConfig.securityToken shouldEqual expectedToken

    ossConfig.newOssClient().getEndpoint shouldEqual expectedFullEndpoint

  }
}
