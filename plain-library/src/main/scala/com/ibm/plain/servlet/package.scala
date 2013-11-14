package com.ibm

package plain

import java.io.File
import java.nio.file.Paths

import com.ibm.plain.io.temporaryDirectory

import config.CheckedConfig

package object servlet

  extends CheckedConfig {

  import config._
  import config.settings._

  final val unpackWebApplicationsToTempDirectory = getBoolean("plain.servlet.unpack-web-applications-to-temp-directory", true)

  final val webApplicationsDirectory = {
    val path = Paths.get(getString("plain.servlet.web-applications-directory"))
    if (path.isAbsolute) path.toFile else Paths.get(config.home).resolve(path).toFile
  }

  final val unpackWebApplicationsDirectory = unpackWebApplicationsToTempDirectory match {
    case true ⇒
      temporaryDirectory
    case false ⇒
      val path = Paths.get(getString("plain.servlet.unpack-web-applications-directory"))
      if (path.isAbsolute) path.toFile else Paths.get(config.home).resolve(path).toFile
  }

}
