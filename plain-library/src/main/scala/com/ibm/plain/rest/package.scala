package com.ibm

package plain

import config.CheckedConfig

package object rest

  extends CheckedConfig {

  import config._
  import config.settings._

  final val maxEntityBufferSize = getBytes("plain.rest.max-entity-buffer-size", 64 * 1024).toInt

}
