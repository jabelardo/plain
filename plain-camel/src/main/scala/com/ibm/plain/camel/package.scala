package com.ibm

package plain

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

  final val actorSystemName = getString("plain.camel.actor-system-name", "akka-camel-system")

  final val actorInvocationTimeout = getMilliseconds("plain.camel.actor-invocation-timeout", 1000)

  final val delayDuringShutdown = getMilliseconds("plain.camel.delay-during-shutdown", 200)

  final val bootstrapOrder = getInt("plain.camel.bootstrap-order", -1)

  final def camelExtension = {
    require(null != camelextension, "plain-camel is not initialized.")
    camelextension
  }

  @volatile private[camel] var camelextension: AkkaCamel = null

}
