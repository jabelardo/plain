package com.ibm.plain

package lib

import java.nio.charset.Charset
import config.CheckedConfig

package object http

  extends CheckedConfig {

  import config._
  import config.settings._

  final val port = getInt("plain.http.port", 7500)

  final val backlog = getInt("plain.http.backlog", 10000)

  final val treadVersion10As11 = getBoolean("plain.http.feature.allow-version-10-but-tread-it-like-11", false)

  final val treadAnyVersionAs11 = getBoolean("plain.http.feature.allow-any-version-but-tread-it-like-11", false)

  final val defaultCharacterSet = Charset.forName(getString("plain.http.feature.default-character-set", "UTF-8"))

  final val disableUrlDecoding = getBoolean("plain.http.feature.diable-url-decoding", false)

}
