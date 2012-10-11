package com.ibm.plain

package lib

package reflect

import org.junit.Assert.assertTrue
import org.junit.Test

@Test class TestReflect {

  @Test def testPrimitives = {
    assertTrue(primitive(classOf[java.lang.Boolean]) == classOf[Boolean])
    assertTrue(primitive(classOf[java.lang.Byte]) == classOf[Byte])
    assertTrue(primitive(classOf[java.lang.Character]) == classOf[Char])
    assertTrue(primitive(classOf[java.lang.Short]) == classOf[Short])
    assertTrue(primitive(classOf[java.lang.Integer]) == classOf[Int])
    assertTrue(primitive(classOf[java.lang.Long]) == classOf[Long])
    assertTrue(primitive(classOf[java.lang.Float]) == classOf[Float])
    assertTrue(primitive(classOf[java.lang.Double]) == classOf[Double])
    assertTrue(primitive(classOf[java.lang.String]) == classOf[String])
  }

  @Test def testBoxed = {
    assertTrue(boxed(classOf[Boolean]) == classOf[java.lang.Boolean])
    assertTrue(boxed(classOf[Byte]) == classOf[java.lang.Byte])
    assertTrue(boxed(classOf[Char]) == classOf[java.lang.Character])
    assertTrue(boxed(classOf[Short]) == classOf[java.lang.Short])
    assertTrue(boxed(classOf[Int]) == classOf[java.lang.Integer])
    assertTrue(boxed(classOf[Long]) == classOf[java.lang.Long])
    assertTrue(boxed(classOf[Float]) == classOf[java.lang.Float])
    assertTrue(boxed(classOf[Double]) == classOf[java.lang.Double])
    assertTrue(primitive(classOf[String]) == classOf[java.lang.String])
  }

}