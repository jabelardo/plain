package com.ibm

package plain

package hybriddb

package schema

package column

import org.junit.Assert.assertTrue
import org.junit.Test

import scala.util.Random

import collection.immutable.Sorting._

@Test class TestColumn {

  @Test def test1 = {
    val v = Array("B", "C", "A", "E", "C", "D", "F")
    val b = new IndexedStringColumnBuilder("s", v.length, Ordering[String])
    v.foreach(b.next(_))
    val s = b.get
    s.gt("C").foreach(i ⇒ println(v(i))); println
    s.gteq("C").foreach(i ⇒ println(v(i))); println
    s.lt("C").foreach(i ⇒ println(v(i))); println
    s.lteq("C").foreach(i ⇒ println(v(i))); println
    s.equiv("C").foreach(i ⇒ println(v(i))); println
    assert(true)
  }

  @Test def test2 = {
    val n = 1000
    val v = Array.fill(n) { Random.nextInt(1000000000).toString }
    val b = new IndexedStringColumnBuilder("s", v.length, Ordering[String])
    v.foreach(b.next(_))
    val s = b.get
    println(v.length)
    s.gteq("2091").foreach(i ⇒ println(v(i))); println
    s.lt("2091").foreach(i ⇒ println(v(i))); println
    assert(true)
  }

  @Test def test3 = {
    val n = 10000000
    val v = Array.fill(n) { Random.nextInt(1000000000).toString }
    println(v.length)
    val b = new IndexedStringColumnBuilder("s", v.length, Ordering[String])
    v.foreach(b.next(_))
    val s = b.get
    println(v.length)
    var t = 0L
    val m = 100
    for (i ← 1 to m) {
      t += time.timeNanos {
        val it = s.matches(".*1011")
        var j = 0
        while (it.hasNext && j < 50) { v(it.next); j += 1 }
      }._2
    }
    println("average " + (t / m))
    assert(true)
  }

  @Test def test4 = {
    val n = 1000000
    var t = 0L
    var m = 1
    var s: MemoryMappedColumn[Double] = null
    if (true) {
      var v = Array.fill(n) { Random.nextInt(3) match { case 0 ⇒ 0.0 case 1 ⇒ 1.0 case 2 ⇒ Random.nextDouble } }
      println(v.length)
      v(333) = 3.14
      v(n - 1) = 2.72
      val b = new MemoryMappedColumnBuilder[Double]("s", v.length, "/tmp/column.bin", Some(Ordering[Double]))
      for (i ← 1 to m) {
        t += time.timeNanos {
          for (j ← 0 until n) b.next(v(j))
          s = b.get
          println("s " + s)
        }._2
      }
      println("average " + (t / m))
      v = null
    }
    System.gc
    t = 0L
    m = 1000000
    for (i ← 1 to m) {
      t += time.timeNanos {
        s(Random.nextInt(n / 100))
      }._2
    }
    println("average " + (t / m))
    assert(s(333) == 3.14)
    assert(s(n - 1) == 2.72)
    t = 0L
    m = 1
    for (i ← 1 to m) {
      for (j ← 0 until n) t += time.timeNanos { s(j) }._2
    }
    println("average " + (t / n))
    assert(true)
  }

  @Test def test5 = {
    val n = 5000000
    var t = 0L
    var m = 1
    var s: MemoryCompressedColumn[Double] = null
    if (true) {
      var v = Array.fill(n) { Random.nextInt(3) match { case 0 ⇒ 0.0 case 1 ⇒ 1.0 case 2 ⇒ Random.nextDouble } }
      println(v.length)
      v(333) = 3.14
      v(n - 1) = 2.72
      val b = new MemoryCompressedColumnBuilder[Double]("s", v.length)
      for (i ← 1 to m) {
        t += time.timeNanos {
          for (j ← 0 until n) b.next(v(j))
          s = b.get
          println("s " + s)
        }._2
      }
      println("average " + (t / m))
      v = null
    }
    System.gc
    t = 0L
    m = 1000000
    for (i ← 1 to m) {
      t += time.timeNanos {
        s(Random.nextInt(n / 100))
      }._2
    }
    println("average " + (t / m))
    assert(s(333) == 3.14)
    assert(s(n - 1) == 2.72)
    t = 0L
    m = 1
    for (i ← 1 to m) {
      for (j ← 0 until n) t += time.timeNanos { s(j) }._2
    }
    println("average " + (t / n))
    assert(true)
  }

}