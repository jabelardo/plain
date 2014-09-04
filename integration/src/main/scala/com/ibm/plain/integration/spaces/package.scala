package com.ibm.plain
package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

import java.nio.file.{ Files, Paths }
import java.net.URI

/**
 *
 */
package object spaces

    extends CheckedConfig {

  import config._
  import config.settings._

  final val spacesConfig = getConfigList("plain.rest.default-dispatcher.routes")

  final val isEnabled = getBoolean("plain.integration.spaces.is-enabled", false)

  final val isClientEnabled = getBoolean("plain.integration.spaces.client.is-enabled", false)

  final val requestTimeout = getMilliseconds("plain.integration.spaces.client.request-timeout", 1 * 60 * 60 * 1000).toInt

  final val rootDirectory = {
    val root = Paths.get(getString("plain.integration.spaces.root-directory", "target/spaces"))
    io.createDirectory(root)
    root
  }

  final val spaceslist = {
    val list = getConfigList("plain.integration.spaces.spaces", List.empty)
    try {
      list.map { c ⇒
        Space(
          c.getString("name"),
          URI.create(c.getString("server-uri").stripSuffix("/")),
          c.getBytes("quota-in-bytes", -1),
          c.getBoolean("purge-on-startup", false),
          c.getBoolean("purge-on-shutdown", false))
      }
    } catch {
      case e: Throwable ⇒
        println("spaces : " + list.mkString("\n"))
        throw e
    }
  }

  final val downloadEncoding = getString("plain.integration.spaces.download-encoding", "gzip")

}
