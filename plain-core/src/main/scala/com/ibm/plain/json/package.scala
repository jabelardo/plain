package com.ibm

package plain

import config.CheckedConfig
import scala.collection.JavaConversions._

import com.sun.jersey.api.json.{ JSONConfiguration, JSONJAXBContext }

package object json

    extends CheckedConfig {

  import config.settings._
  import config._

  final val formattedOutput = getBoolean("plain.json.formatted-output", false)

  final val encodeOutput = getBoolean("plain.json.encode-output", true)

  final val jsonparser = Json

  final val unmarshalJson = JsonMarshaled

  final val jsonconfiguration = JSONConfiguration
    .natural
    .rootUnwrapping(false)
    .humanReadableFormatting(formattedOutput)
    .build

}
