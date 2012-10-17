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

  final val treat10VersionAs11 = getBoolean("plain.http.feature.allow-version-1.0-but-treat-it-like-1.1", false)

  final val treatAnyVersionAs11 = getBoolean("plain.http.feature.allow-any-version-but-treat-it-like-1.1", false)

  final val defaultCharacterSet = Charset.forName(getString("plain.http.feature.default-character-set", "UTF-8"))

  final val disableUrlDecoding = getBoolean("plain.http.feature.diable-url-decoding", false)

}
