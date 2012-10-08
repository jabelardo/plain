package com.ibm.plain

package lib

import config.CheckedConfig
import scala.collection.JavaConversions._

import com.sun.jersey.api.json.{ JSONConfiguration, JSONJAXBContext }

package object json

  extends CheckedConfig {

  import config.settings._

  final val formattedOutput = getBoolean("plain.json.formatted-output")

  final val jsonparser = Json

  final val unmarshalJson = JsonMarshaled

  final val jsonconfiguration = JSONConfiguration
    .natural
    .rootUnwrapping(false)
    .humanReadableFormatting(formattedOutput)
    .build

}
