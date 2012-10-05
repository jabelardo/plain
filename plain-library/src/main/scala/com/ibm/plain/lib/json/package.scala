package com.ibm.plain

package lib

import config.CheckedConfig
import scala.collection.JavaConversions._

package object json

  extends CheckedConfig

  with logging.HasLogger {

  import log._

  def test = {

    val source = """{ "result" : { "data" : [ 1, "name", true, null, { "name" : "value" } ], "status" : 0 } }"""

    println(source)

    val j = Json.parse(source)

    println(j)
    println(j.toString)

    info(j.asObject("result").asObject("data").asArray(2).getClass.toString)
    info(j.asObject("result").asObject("data").asArray(2).asBoolean.toString)
    info("short form " + j("result")("data")(2).asBoolean)
    info("short form " + j("result")("data")(4)("name"))
    info(Json.build(j))

  }

}