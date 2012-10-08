package com.ibm.plain

package lib

package text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import crypt.Uuid
import crypt.Crypt
import crypt.MD5

@Test private class TextTest {

  val print = false

  @Test def testConverters = {
    assertTrue(isNumber("1"))
    assertTrue(isNumber("42"))
    assertTrue(isNumber("0.0"))
    assertTrue(isNumber("0."))
    assertTrue(isNumber("10000000000000000"))
    assertTrue(isNumber("3.1415"))
    assertTrue(isNumber("-1"))
    assertTrue(isNumber("-2.15"))
    assertTrue(isNumber("-0"))
    assertFalse(isNumber("hello"))
    assertFalse(isNumber("0.000.1"))
    assertFalse(isNumber("10000000000000000L"))

    var start = System.nanoTime
    for (i ← 1 to 100) {
      assertTrue("abc" == unhexify(hexify("abc")))
      assertTrue("abc" == unhexify(hexify("ABC")).toLowerCase)
      assertTrue(s == unhexify(hexify(unhexify(hexify(s)))))
      assertTrue(longs == unhexify(hexify(longs)))
      assertTrue(unhexify("6A6B6C6D") == unhexify("6a6b6c6d"))
      assertTrue(muenchen == unhexify(hexify(muenchen)))
    }
    var end = System.nanoTime
    if (print) println("hexstring : " + (end - start) / 1000000000.0 + " sec")

    start = System.nanoTime
    for (i ← 1 to 10) {
      assertTrue("abc" == unhexifyCrypted(hexifyCrypted("abc")))
      assertTrue(s == unhexifyCrypted(hexifyCrypted(unhexifyCrypted(hexifyCrypted(s)))))
      assertTrue(longs == unhexifyCrypted(hexifyCrypted(longs)))
      assertTrue(unhexifyCrypted("6A6B6C6D") == unhexifyCrypted("6a6b6c6d"))
      assertTrue(muenchen == unhexifyCrypted(hexifyCrypted(muenchen)))
    }
    end = System.nanoTime
    if (print) println("crypthexstring : " + (end - start) / 1000000000.0 + " sec")
  }

  @Test def testUuid = {
    val u = Uuid.newUuid
    val s: String = Uuid.newUuid.toUpperCase
    assertTrue(32 == s.length)
  }

  @Test def testMD5 = {
    assertTrue("19ff680c1fa57aafb74292dfda79ebc2" == MD5(s))
    assertTrue("eebfe82abd231302d8c0ef76f1b6b7cb" == MD5(longs))
    assertTrue("bb9956e992316cb90fa166a345838506" == MD5(muenchen))
    assertFalse("afd66d987d45d6b67408eb2ef75b6995" == MD5(muenchen))
    assertTrue(32 == MD5(s).length)
    assertTrue(32 == MD5(longs).length)
    assertTrue(32 == MD5(muenchen).length)
  }

  @Test def testCrypt = {
    assertTrue("AAdFMPiEiQmQw" == Crypt.crypt(null, "Hello world."))
    assertTrue("so39caW9.TBUk" == Crypt.crypt("somesalt", "Hello world."))
  }

  val longs = "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz%$23456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz[]23456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz{}23456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz()23456789abcdefghijklmnopqrstuvwxyz0123456789abcdeFGHIJKlmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0=23456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz!?23456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789"
  val s = "This is a test string 0123456789 ABC@!$%&/"
  val muenchen = "M\u00fcnchen und N\u00fcrnberg!"

}