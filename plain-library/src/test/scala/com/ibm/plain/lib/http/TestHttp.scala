package com.ibm.plain

package lib

package http

import scala.concurrent.util.duration.intToDurationInt

import lib.run

import org.junit.Test

@Test class TestHttp {

  @Test def testA = {
    println("TestHttp")
    run(15.seconds) {
      HttpTest.apply
      lib.application.teardown
    }
    assert(true)
  }

}

