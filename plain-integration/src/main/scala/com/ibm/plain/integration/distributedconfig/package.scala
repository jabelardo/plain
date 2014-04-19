package com.ibm

package plain

package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

/**
 *
 */
package object distributedconfig

  extends CheckedConfig {

  import config._
  import config.settings._

  final val bootstrapOrder = getInt("plain.integration.distributed-config.bootstrap-order", -1)

  final val udpPort = getInt("plain.integration.distributed-config.udp-port", 50499)

  final val isMaster = getBoolean("plain.integration.distributed-config.is-master", false)

  final val polling = getBoolean("plain.integration.distributed-config.polling", true)

  final val pollingTimeout = getMilliseconds("plain.integration.distributed-config.polling-timeout", 15000)

  final val startDelayedTimeout = getMilliseconds("plain.integration.distributed-config.start-delayed-timeout", 5000)

}
