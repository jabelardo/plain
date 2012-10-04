package com.ibm.plain

package lib

import com.typesafe.config.{ ConfigFactory, Config }

package object config

  extends CheckedConfig {

  final val settings: Config = {
    val config = ConfigFactory.load
    config.checkValid(config, "plain")
    config
  }

  import settings._

  final val version = getString("plain.config.version")

  final val logConfigOnStart = getBoolean("plain.config.log-config-on-start")

  final val printStackTraceOnError = getBoolean("plain.config.print-stacktrace-on-error")

  final val terminateOnError = getBoolean("plain.config.terminate-on-error")

  final val terminateOnErrorExitCode = getInt("plain.config.terminate-on-error-exitcode")

  if (logConfigOnStart) println(root.render)

}
