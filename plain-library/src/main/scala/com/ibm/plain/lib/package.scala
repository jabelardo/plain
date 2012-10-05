package com.ibm.plain

package object lib

  extends config.CheckedConfig {

  import config._
  import config.settings._

  override lazy val toString = root.render

  final val requiredversion = "1.0.1"

  final val home = getString("plain.home", System.getenv("PLAIN_HOME"))

  /**
   * check requirements
   */
  require(null != home, "Neither plain.home config setting nor PLAIN_HOME environment variable are set.")
  require(requiredversion == config.version, String.format("plain.version (%s) does not match internal version (%s).", config.version, requiredversion))

}

