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

  private[this] final val rconfig = new RichConfig(settings)

  final val logConfigOnStart = rconfig.getBoolean("plain.config.log-config-on-start", false)

  final val logConfigFormatted = rconfig.getBoolean("plain.config.log-config-formatted", false)

  final val rethrowExceptionOnError = rconfig.getBoolean("plain.config.rethrow-exception-on-error", false)

  final val printStackTraceOnError = rconfig.getBoolean("plain.config.print-stacktrace-on-error", true)

  final val terminateOnError = rconfig.getBoolean("plain.config.terminate-on-error", true)

  final val terminateOnErrorExitCode = rconfig.getInt("plain.config.terminate-on-error-exitcode", -1)

  /**
   * Must match the version string provided by the *.conf files.
   */
  final val requiredVersion = "1.0.0"

  final val home = rconfig.getString("plain.config.home", System.getenv("PLAIN_HOME"))

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
