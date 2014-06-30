package com.ibm

package plain

package io

import java.nio.ByteBuffer
import scala.language.implicitConversions
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileOutputStream

/* @Test */ class IoTest {

  /*
    val print = false

  /* @Test */def testNullStream = {
    val nullstream = NullOutputStream
    (1 to 100).foreach(nullstream.write)
    assertTrue("" == nullstream.toString)
  }

  /* @Test */def testBinaryFormat = {
    val array = java.nio.ByteBuffer.allocateDirect(10000)
    if (true) {
      val out = new BinaryFormatByteBuffer(array, 0, 10000)
      val s = "This is a test string."
      val longs = """
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
"""
      val map = Map("a" -> "value1", "b" -> 42, "c" -> 3.1415, "d" -> Map("aa" -> "value2"), "e" -> List(1, "s", false, 1000000000000000L), "f" -> Some(Some(3.toByte)))
      val list = List(1, "s", true, 3.1415, Map("a" -> "string"))
      val a = Array[Byte](1, 2, 3, 4, 5, 6, 7)
      out.writeBoolean(true)
      out.writeBoolean(false)
      out.writeByte(0)
      out.writeByte(255.toByte)
      out.writeShortBytes(a)
      out.writeBytes(a)
      out.writeChar('@')
      out.writeShort(32755)
      out.writeInt(42)
      out.writeInt(100000000)
      out.writeLong(10000000000000L)
      out.writeDouble(3.141592)
      out.writeShortString(s)
      out.writeString(s)
      out.writeString(longs)
      out.writeMap(map)
      out.writeList(list)
      out.writeOption(None)
      out.writeOption(Some("Hello"))

      val in = new BinaryFormatByteBuffer(array, 0, 10000)
      assertTrue(true == in.readBoolean)
      assertTrue(false == in.readBoolean)
      assertTrue(0 == in.readByte)
      assertTrue(255.toByte == in.readByte)
      assertTrue(a.length == in.readShortBytes.length)
      assertTrue(a.length == in.readBytes.length)
      assertTrue('@' == in.readChar)
      assertTrue(32755 == in.readShort)
      assertTrue(42 == in.readInt)
      assertTrue(100000000 == in.readInt)
      assertTrue(10000000000000L == in.readLong)
      assertTrue(3.141592 == in.readDouble)
      assertTrue(s == in.readShortString)
      assertTrue(s == in.readString)
      assertTrue(longs == in.readString)
      assertTrue(map == in.readMap)
      assertTrue(list == in.readList)
      assertTrue(None == in.readOption)
      assertTrue(Some("Hello") == in.readOption)
    }
  }

  /* @Test */def testCopy = {
    val array = new Array[Byte](1000000)
    var out = new ByteArrayOutputStream(array, 0, 1000000)
    val s = Array[Byte](1, 2, 3, 4, 5)
    (1 to 100000).foreach(_ ⇒ out.write(s))
    List(0.0625, 0.5, 2, 16, 32, 51, 64, 128).foreach { b ⇒
      val bufsize = (b * 1024).toInt
      val start = System.nanoTime
      (1 to 100).foreach(j ⇒ {
        val in = out.getInputStream
        assertTrue(500000 == in.available)
        val o = new ByteArrayOutputStream(array, 0, 1000000)
        copyBytes(in, o, bufsize)
        out = o
      })
      val end = System.nanoTime
      if (print) println(bufsize + " : " + (end - start) / 1000000000.0 + " sec")
    }
  }

  /* @Test */def testBase64 = {
    for (i ← 1 to 10) {
      val a1 = new Array[Byte](1000000)
      val a2 = new Array[Byte](1000000)
      val a3 = new Array[Byte](1000000)
      val o1 = new ByteArrayOutputStream(a1, 0, 1000000)
      val o2 = new ByteArrayOutputStream(a2, 0, 1000000)
      val o3 = new ByteArrayOutputStream(a3, 0, 1000000)
      val s = Array[Byte](1, 2, 3, 4, 5)
      (1 to 100000).foreach(_ ⇒ o1.write(s))
      val b64o = new Base64OutputStream(o2)
      copyBytes(o1.getInputStream, b64o)
      val b64i = new Base64InputStream(o2.getInputStream)
      copyBytes(b64i, o3)
      assertTrue(java.util.Arrays.equals(a1, a3))
    }
  }

  /* @Test */def testFileChannel = {
    for (i ← 1 to 10) {
      val i = temporaryFile
      val o = temporaryFile
      val a1 = new Array[Byte](1000000)
      val a3 = new Array[Byte](1000000)
      val o1 = new ByteArrayOutputStream(a1, 0, 1000000)
      val o3 = new ByteArrayOutputStream(a3, 0, 1000000)
      val s = Array[Byte](1, 2, 3, 4, 5)
      (1 to 100000).foreach(_ ⇒ o1.write(s))
      var ii: java.io.InputStream = null
      var oo: java.io.OutputStream = null
      ii = o1.getInputStream
      oo = new java.io.FileOutputStream(i)
      copyBytes(ii, oo)
      ii.close; oo.close
      ii = new java.io.FileInputStream(i)
      oo = new java.io.FileOutputStream(o)
      copyBytes(ii, oo)
      ii.close; oo.close
      ii = new java.io.FileInputStream(o)
      oo = o3
      copyBytes(ii, oo)
      ii.close; oo.close
      assertTrue(java.util.Arrays.equals(a1, a3))
    }

    /* @Test */def testByteBuffer = {
      val longs = """
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789
"""
      val buffer = ByteBuffer.allocateDirect(64 * 1024)
      buffer.put(longs.getBytes)
      buffer.flip
      println(aio.format(buffer))
      gzip(buffer)
      println("after gzip " + aio.format(buffer))
      assertTrue(true)
    }
  }
*/
  /* @Test */ def testLZ4 = {
    import java.io._
    import scala.util.Random
    var t = 0L
    val m = 100
    //    val array = Array.fill(10000000)(Random.nextInt(100).toString.getBytes)
    for (j ← 1 to m) {
      println(j)
      t += time.timeMillis {
        val i = new FileInputStream("/tmp/list.txt")
        val f = new FileOutputStream("/tmp/test.lz4")
        val o = LZ4.newHighOutputStream(f)
        io.copyBytes(i, o)
        i.close
        o.close
      }._2
    }
    println("average = " + (t / m))
    t = 0L
    for (j ← 1 to m) {
      t += time.timeMillis {
        val f = new FileInputStream("/tmp/test.lz4")
        val i = LZ4.newInputStream(f)
        val buf = new Array[Byte](64 * 1024)
        var total = 0L
        while (-1 < { val len = i.read(buf); total += len; len }) ()
        i.close
      }._2
    }
    println("average = " + (t / m))
  }

}
