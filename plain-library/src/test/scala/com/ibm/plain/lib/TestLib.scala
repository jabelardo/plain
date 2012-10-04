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
    import logging._
    log.debug("debug")
    log.info("info")
    log.warning("warning")
    log.error("error")
    new Thread(new Runnable { def run = { Thread.sleep(500); shutdown } }).start
    awaitTermination
    assert(true)
  }

}

