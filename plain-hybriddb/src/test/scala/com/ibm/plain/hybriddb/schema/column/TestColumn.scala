package com.ibm

package plain

package hybriddb

package schema

package column

import org.junit.Assert.assertTrue
import org.junit.Test

import scala.util.Random

@Test class TestColumn {

  @Test def test1 = {
    val v = Array("B", "A", "E", "C", "D", "F")
    val s = new IndexedStringColumn(v.length, v, Ordering[String])
    s.>("A").foreach(i ⇒ println(v(i))); println
    s.>=("A").foreach(i ⇒ println(v(i))); println
    s.<("A").foreach(i ⇒ println(v(i))); println
    s.<=("A").foreach(i ⇒ println(v(i))); println

    assert(true)
  }

  @Test def test2 = {
    val n = 1000
    val v = Array.fill(n) { Random.nextInt(1000000000).toString }
    v(333) = "209160821"
    val s = new IndexedStringColumn(v.length, v, Ordering[String])
    println(v.length)
    println(v.toList)
    s.>=("209160821").foreach(i ⇒ println(v(i))); println

    assert(true)
  }

}