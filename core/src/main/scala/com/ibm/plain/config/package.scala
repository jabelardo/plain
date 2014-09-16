package com.ibm.plain

import scala.language.implicitConversions

import com.typesafe.config.{ Config, ConfigFactory }

/**
 *
 */
package object config

    extends config.CheckedConfig {

  /**
   * The "global" plain application configuration settings.
   */
  final val settings: RichConfig = {
    val config = ConfigFactory.load
    config.checkValid(config, "plain")
    new RichConfig(config)
  }

  import settings._

  final val version = settings.getString("plain.config.version")

  final val logConfigOnStart = settings.getBoolean("plain.config.log-config-on-start", false)

  final val logConfigFormatted = settings.getBoolean("plain.config.log-config-formatted", false)

  final val rethrowExceptionOnError = settings.getBoolean("plain.config.rethrow-exception-on-error", false)

  final val printStackTraceOnError = settings.getBoolean("plain.config.print-stacktrace-on-error", true)

  final val terminateOnError = settings.getBoolean("plain.config.terminate-on-error", true)

  final val terminateOnErrorExitCode = settings.getInt("plain.config.terminate-on-error-exitcode", -1)

  /**
   * Must match the version string provided by the *.conf files.
   */
  final val requiredVersion = "1.0"

  final val home = settings.getString("plain.config.home", System.getenv("PLAIN_HOME"))

  /**
   * implicit conversions
   */

  implicit def config2RichConfig(config: Config) = new RichConfig(config)

  /**
   * check requirements
   */
  require(null != home, "Neither plain.config.home config setting nor PLAIN_HOME environment variable are set.")

  require(requiredVersion == config.version, String.format("plain.config.version in *.conf files (%s) does not match internal version (%s).", config.version, requiredVersion))

}
