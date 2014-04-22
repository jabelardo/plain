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

}
