package com.ibm.plain.lib

import org.junit.Test

object TestLib {

  val i = 0

  concurrent.startup

  override protected def finalize = concurrent.shutdown

}

@Test class TestLib {

  import TestLib._

  val x = i

  @Test def testA = {
    import logging._
    log.info("info")
    println(requiredversion)
    assert(true)
  }

  @Test def testB = {
    import os._
    println(operatingSystem)
    println(username)
    println(hostname)
    assert(true)
  }

  @Test def testC = {
    import io._
    val f = temporaryFile
    val d = temporaryDirectory
    println(f)
    println(d)
  }

  @Test def testD = {
    import concurrent._
    import logging.log._
    println(actorSystem)
    debug("debug")
    info("info")
    warning("warning")
    error("error")
    spawn { sleep(200000); shutdown }
    var c = 0
    schedule(1000, 2000) {
      c += 1
      debug("debug " + c)
      info("info " + c)
      warning("warning " + c)
      error("error " + c)
    }
    awaitTermination
    assert(true)
  }

  @Test def testF = {
    import json._
    jsonparser("""{"name":"value"}""")
    assert(true)
  }

}

