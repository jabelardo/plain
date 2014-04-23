package com.ibm

package plain

package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

/**
 *
 */
package object activemq

  extends CheckedConfig {

  import config._
  import config.settings._

  final val bootstrapOrder = getInt("plain.integration.activemq.bootstrap-order", -1)

  final val brokerServerUri = getString("plain.integration.activemq.broker-server-uri", "tcp://0.0.0.0")

  final val brokerClientUri = getString("plain.integration.activemq.broker-client-uri", "tcp://localhost")

  final val brokerPort = getInt("plain.integration.activemq.broker-port", 61616)

  final val isMaster = getBoolean("plain.integration.activemq.is-master", false)

  final val isEnabled = getBoolean("plain.integration.activemq.is-enabled", false)

}
