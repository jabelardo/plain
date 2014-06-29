package com.ibm

package plain

package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

import java.nio.file.{ Files, Paths }
/**
 *
 */
package object spaces

    extends CheckedConfig {

  import config._
  import config.settings._

  final val spacesConfig = getConfigList("plain.rest.default-dispatcher.routes")

  final val isEnabled = getBoolean("plain.integration.spaces.is-enabled", false)

  final val rootDirectory = {
    val root = Paths.get(getString("plain.integration.spaces.root-directory", "target/spaces"))
    io.createDirectory(root)
    root
  }

  final val spaceslist = getConfigList("plain.integration.spaces.spaces", List.empty).map { c ⇒
    Space(c.getString("name"), c.getBytes("quota-in-bytes", -1), c.getBoolean("purge-on-startup", false), c.getBoolean("purge-on-shutdown", false))
  }

  final val downloadEncoding = getString("plain.integration.spaces.download-encoding", "deflate")

}
