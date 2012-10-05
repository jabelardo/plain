package com.ibm.plain.lib

import org.junit.Test

@Test class TestLib {

  @Test def testA = {
    import logging._
    log.debug("debug")
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
    spawn { Thread.sleep(1000); shutdown }
    awaitTermination
    assert(true)
  }

  @Test def testF = {
    import json._
    test
  }

}

