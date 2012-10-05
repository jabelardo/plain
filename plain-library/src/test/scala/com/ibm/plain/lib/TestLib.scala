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
    assert(true)
  }

  @Test def testC = {
    import concurrent._
    import logging.log._
    println(actorSystem)
    debug("debug")
    info("info")
    warning("warning")
    error("error")
    spawn { Thread.sleep(500); println("shutdown"); shutdown }
    awaitTermination
    assert(true)
  }

}

