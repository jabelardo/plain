package com.ibm.plain.lib

import org.junit.Test

@Test class TestLib {

  @Test def testA = {
    println("before")
    println("version " + config.version)
    println("x " + x)
    println("after")
  }

}

