package com.ibm

package plain

import org.apache.commons.codec.net.URLCodec

import config.CheckedConfig

package object http

    extends CheckedConfig {

  import config._
  import config.settings._

  final val startupServers: List[String] = getStringList("plain.http.startup-servers", List.empty)

  final val defaultCharacterSet = text.`ISO-8859-15`

  final val defaultcodec = new URLCodec(defaultCharacterSet.toString)

  final lazy val defaultServerConfiguration = new Server.ServerConfiguration("plain.http.default-server", true)

  final val channelGroupThreadPoolType = getInt("plain.http.channel-group-thread-pool-type", 3)

  /**
   * Set to true in specific benchmark situations only.
   */
  final val ignoreAcceptHeader = getBoolean("plain.http.ignore-accept-header", false)

  final val ignoreEntityEncoding = getBoolean("plain.http.ignore-entity-encoding", false)

  final val maxLengthArrayEntity = getBytes("plain.http.max-length-array-entity", 32 * 1024).toInt

}
