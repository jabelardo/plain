package com.ibm

package plain

import config.CheckedConfig

package object jdbc

  extends CheckedConfig {

  import config._
  import config.settings._

  final val startupConnectionFactories: List[String] = getStringList("plain.jdbc.startup-connection-factories", List.empty)

}