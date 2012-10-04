package com.ibm.plain

package object lib

  extends config.CheckedConfig {

  import config.settings._

  final val requiredversion = "1.0.1"

  if (requiredversion != config.version) throw new IllegalArgumentException(String.format("plain.version (%s) does not match internal version (%s).", config.version, requiredversion))

  val x = 1 / 1

  val home = getString("plain.home")

}

