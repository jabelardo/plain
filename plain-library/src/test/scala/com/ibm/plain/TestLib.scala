package com.ibm

package plain

import scala.concurrent.duration._

import org.junit.Test

import concurrent.schedule
import logging.defaultLogger
import os._
import json._
import io._
import http._
import config._

@Test class TestLib {

  @Test def testA = {
    run(1.minute) {
      println(requiredVersion)
      println(operatingSystem)
      println(userName)
      println(hostName)
      jsonparser("""{"name":"value"}""")
      val f = temporaryFile
      val d = temporaryDirectory
      println(f)
      println(d)
      defaultLogger.debug("debug")
      defaultLogger.info("info")
      defaultLogger.warning("warning")
      defaultLogger.error("error")
      var c = 0
      schedule(1000, 2000) {
        c += 1
        println("print " + c)
        val log = defaultLogger
        import log._
        debug("debug " + c)
        info("info " + c)
        warning("warning " + c)
        error("error " + c)
      }
      println("after serve")
    }
  }

}

