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
    val s = new IndexedColumn[String](v.length, v, Ordering[String])
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
    val s = new IndexedColumn[String](v.length, v, Ordering[String])
    println(v.length)
    s.gteq("2091").foreach(i ⇒ println(v(i))); println
    s.lt("2091").foreach(i ⇒ println(v(i))); println
    assert(true)
  }

  @Test def test3 = {
    val n = 10000000
    val v = Array.fill(n) { Random.nextInt(1000000000).toString }
    println(v.length)
    val s = new IndexedStringColumn(v.length, v, Ordering[String])
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

}