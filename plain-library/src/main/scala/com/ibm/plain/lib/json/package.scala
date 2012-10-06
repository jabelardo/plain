package com.ibm.plain

package lib

import config.CheckedConfig
import scala.collection.JavaConversions._

package object json

  extends CheckedConfig

  with logging.HasLogger {

  import java.io._
  import time._

  def test = {

    val source = """{ "result" : { "data" : [ 1, "name", true, null, { "name" : "value" } ], "status" : 0 } }"""

    println(source)

    val j = Json.parse(source)

    println(j)
    println(j.toString)

    var l = 0L
    info(j.asObject("result").asObject("data").asArray(2).getClass.toString)
    info(j.asObject("result").asObject("data").asArray(2).asBoolean.toString)
    info("short form " + j("result")("data")(2).asBoolean)
    infoNanos("how long?")(info("short form " + j("result")("data")(4)("name")))
    info(Json.build(j))

    for (i ← 1 to 10) {
      infoMillis {
        val s = Json.parse(new FileReader("/tmp/big.json"))
        // debug("{}", s.toString.length)
      }
    }
    println
  }

}