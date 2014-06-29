package com.ibm

package plain

package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

/**
 *
 */
package object infrastructure

    extends CheckedConfig {

  import config._
  import config.settings._

  final val isEnabled = getBoolean("plain.integration.infrastructure.is-enabled", false)

}
