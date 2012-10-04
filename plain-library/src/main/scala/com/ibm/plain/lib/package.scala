package com.ibm.plain

import java.nio.file.{ Files, Paths }

package object lib

  extends config.CheckedConfig {

  import config._
  import config.settings._

  override lazy val toString = root.render

  final val requiredversion = "1.0.1"

  final val home = getString("plain.home", System.getenv("PLAIN_HOME"))

  final val temp = try {
    val tmp = getString("plain.temp", System.getenv("TMP"))
    Files.createDirectories(Paths.get(tmp))
    System.setProperty("java.io.tmpdir", tmp)
    tmp
  } catch {
    case _: Throwable ⇒
      System.getProperty("java.io.tmpdir")
  }

  /**
   * check requirements
   */
  require(null != home, "Neither plain.home config setting nor PLAIN_HOME environment variable are set.")
  require(null != temp, "Neither plain.temp config setting nor TMP environment variable nor java.io.tmpdir property are set.")
  require(requiredversion == config.version, String.format("plain.version (%s) does not match internal version (%s).", config.version, requiredversion))

}

