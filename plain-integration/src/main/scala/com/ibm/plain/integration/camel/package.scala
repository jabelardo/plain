package com.ibm

package plain

package integration

import org.apache.camel.CamelContext

import scala.language.implicitConversions

import config.CheckedConfig

/**
 *
 */
package object camel

  extends CheckedConfig {

  import config._
  import config.settings._

  final val delayDuringShutdown = getMilliseconds("plain.integration.camel.delay-during-shutdown", 200)

  final val bootstrapOrder = getInt("plain.integration.camel.bootstrap-order", -1)

  final val servletServicesRoot = getString("plain.integration.camel.servlet-services-root", "integration-services")

  final def context = {
    require(null != camelcontext, "plain-integration-camel is not initialized.")
    camelcontext
  }

  @volatile private[camel] var camelcontext: CamelContext = null

}
