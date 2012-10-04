package com.ibm.plain.lib

import org.junit.Test

@Test class TestLib {

  @Test def testA = {
    val v = requiredversion
    val y = x
    println(y)
    assert(true)
  }

  @Test def testB = {
    assert(true)
  }

}

