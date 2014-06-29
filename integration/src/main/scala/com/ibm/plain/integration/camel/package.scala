package com.ibm

package plain

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

  final val delayDuringShutdown = getMilliseconds("plain.integration.camel.delay-during-shutdown", 200)

  final val servletServicesRoot = getString("plain.integration.camel.servlet-services-root", "integration-services")

  final val isEnabled = getBoolean("plain.integration.camel.is-enabled", false)

}
