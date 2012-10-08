package com.ibm.plain.lib

import org.junit.Test

@Test class TestLib {

  override protected def finalize = {
    concurrent.shutdown
  }

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
    assert(true)
    spawn { sleep(2000); shutdown }
    awaitTermination
  }

  @Test def testF = {
    import json._
    jsonparser("""{"name":"value"}""")
    assert(true)
  }

}

