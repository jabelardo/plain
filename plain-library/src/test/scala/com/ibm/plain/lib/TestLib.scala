package com.ibm.plain.lib

import org.junit.Test

@Test class TestLib {

  @Test def testA = {
    val v = requiredversion
    val y = x
    println(y)
    println(home)
    assert(true)
  }

  @Test def testB = {
    assert(true)
  }

}

