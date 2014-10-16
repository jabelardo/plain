package com.ibm.plain
package integration

import scala.language.implicitConversions

import config.{ CheckedConfig, config2RichConfig }

/**
 *
 */
package object camel

    extends CheckedConfig {

  import config._
  import config.settings._

  final val shutdownTimeout = getMilliseconds("plain.integration.camel.shutdown-timeout", 5000)

  final val delayDuringShutdown = getMilliseconds("plain.integration.camel.delay-during-shutdown", 500)

  final val servletServicesRoot = getString("plain.integration.camel.servlet-services-root", "integration-services")

  final val isEnabled = getBoolean("plain.integration.camel.is-enabled", false)

}
