package com.ibm.plain

package lib

import config.CheckedConfig

package object http

  extends CheckedConfig {

  import config._
  import config.settings._

  final val defaultServerConfiguration = Server.ServerConfiguration("plain.http.default-server", true)

  final val startupServers: List[String] = getStringList("plain.http.startup-servers", List.empty)

}
