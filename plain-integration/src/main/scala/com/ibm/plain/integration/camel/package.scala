package com.ibm

package plain

package integration

import scala.language.implicitConversions

import akka.camel.{ Camel ⇒ AkkaCamel }
import config.CheckedConfig

/**
 *
 */
package object camel

  extends CheckedConfig {

  import config._
  import config.settings._

  final val actorSystemName = getString("plain.integration.camel.actor-system-name", "plain-camel-system")

  final val actorInvocationTimeout = getMilliseconds("plain.integration.camel.actor-invocation-timeout", 15000)

  final val delayDuringShutdown = getMilliseconds("plain.integration.camel.delay-during-shutdown", 200)

  final val bootstrapOrder = getInt("plain.integration.camel.bootstrap-order", -1)

  final val servletServicesRoot = getString("plain.integration.camel.servlet-services-root", "integration-services")

  final def camelExtension = {
    require(null != camelextension, "plain-integration-camel is not initialized.")
    camelextension
  }

  @volatile private[camel] var camelextension: AkkaCamel = null

}
