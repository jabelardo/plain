package com.ibm

package plain

import scala.language.implicitConversions

import com.typesafe.config.{ Config, ConfigFactory }

package object config

  extends config.CheckedConfig {

  /**
   * The "global" plain application configuration settings.
   */
  final val settings: Config = {
    val config = ConfigFactory.load
    config.checkValid(config, "plain")
    config
  }

  import settings._

  final val version = getString("plain.config.version")

  final val logConfigOnStart = getBoolean("plain.config.log-config-on-start")

  final val rethrowExceptionOnError = getBoolean("plain.config.rethrow-exception-on-error")

  final val printStackTraceOnError = getBoolean("plain.config.print-stacktrace-on-error")

  final val terminateOnError = getBoolean("plain.config.terminate-on-error")

  final val terminateOnErrorExitCode = getInt("plain.config.terminate-on-error-exitcode")

  /**
   * implicit conversions
   */

  implicit def config2RichConfig(config: Config) = new RichConfig(config)

  /**
   * Must match the version string provided by the *.conf files.
   */
  final val requiredVersion = "1.0.0"

  final val home = getString("plain.config.home", System.getenv("PLAIN_HOME"))

  /**
   * check requirements
   */
  require(null != home, "Neither plain.config.home config setting nor PLAIN_HOME environment variable are set.")

  require(requiredVersion == config.version, String.format("plain.config.version in *.conf files (%s) does not match internal version (%s).", config.version, requiredVersion))

}
