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

  final val brokerServerUri = getString("plain.integration.activemq.broker-server-uri", "tcp://0.0.0.0")

  final val brokerClientUri = getString("plain.integration.activemq.broker-client-uri", "tcp://localhost")

  final val brokerPort = getInt("plain.integration.activemq.broker-port", 61616)

  final val usePersistence = getBoolean("plain.integration.activemq.use-persistence", true)

  final val persistenceDirectory = getString("plain.integration.activemq.persistence-directory", "target/activemq-persistence")

  final val purgePersistenceDirectoryOnStartup = getBoolean("plain.integration.activemq.purge-persistence-directory-on-startup", false)

  final val isMaster = getBoolean("plain.integration.activemq.is-master", false)

  final val isEnabled = getBoolean("plain.integration.activemq.is-enabled", false)

}
