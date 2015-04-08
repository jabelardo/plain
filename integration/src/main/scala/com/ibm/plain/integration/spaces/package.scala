package com.ibm.plain
package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig
import logging.Logger

import java.io.IOException
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

  final val requestTimeout = getMilliseconds("plain.integration.spaces.client.request-timeout", 15 * 60 * 1000).toInt

  final val rootDirectory = {
    val root = Paths.get(getString("plain.integration.spaces.root-directory", "target/spaces"))
    io.createDirectory(root)
    root
  }

  final val fallbackDirectory = {
    val fallback = Paths.get(getString("plain.integration.spaces.fallback-directory", rootDirectory.resolve("fallback").toString))
    io.createDirectory(fallback)
    fallback
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

  final val useConduitsDefault = getBoolean("plain.integration.spaces.use-conduits-default", false)

  final val minimumFileSpaceNecessary = getBytes("plain.integration.spaces.minimum-filespace-necessary", 1 * 1024 * 1024 * 1024) // default: 1 gb

  final def checkMinimumFileSpace = {
    def check(path: java.nio.file.Path) = {
      val available = rootDirectory.toFile.getUsableSpace
      if (available < minimumFileSpaceNecessary) {
        error(s"""
//**********************************************************************
// We ran out of file space, again!
// Path      : $rootDirectory
// Available : $available bytes
// Threshold : $minimumFileSpaceNecessary bytes
//**********************************************************************
""")
        throw new IOException(s"No space left on device. Path: $rootDirectory")
      }
    }
    check(rootDirectory)
    check(io.temp)
  }

}
