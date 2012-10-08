package com.ibm.plain.lib

import org.junit.Test

import concurrent._
import logging.log._
import os._
import json._
import io._
import http._
import http.HttpAio._

@Test class TestLib {

  @Test def testA = {
    plain {
      println(requiredversion)
      println(operatingSystem)
      println(username)
      println(hostname)
      jsonparser("""{"name":"value"}""")
      val f = temporaryFile
      val d = temporaryDirectory
      println(f)
      println(d)
      println(actorSystem)
      debug("debug")
      info("info")
      warning("warning")
      error("error")
      spawn { sleep(200000); concurrent.shutdown }
      var c = 0
      schedule(1000, 2000) {
        c += 1
        println("print " + c)
        debug("debug " + c)
        info("info " + c)
        warning("warning " + c)
        error("error " + c)
      }
      test(server)
      concurrent.awaitTermination
      println("after serve")
    }
  }

}

