package com.ibm

package plain

package integration

import scala.language.implicitConversions

import config.CheckedConfig
import config.settings.getConfig

/**
 *
 */
package object spaces

  extends CheckedConfig {

  import config._
  import config.settings._

  final val bootstrapOrder = getInt("plain.integration.spaces.bootstrap-order", -1)

  final val spacesConfig = getConfigList("plain.rest.default-dispatcher.routes")

  final val isEnabled = getBoolean("plain.integration.spaces.is-enabled", false)

}
