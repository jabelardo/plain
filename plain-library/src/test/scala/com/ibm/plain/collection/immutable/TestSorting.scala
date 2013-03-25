package com.ibm

package plain

package collection

package immutable

import org.junit.Assert.assertTrue
import org.junit.Test
import scala.util.Random

// add @Test for testing

final class TestSortedArray {

  @Test def test1 = {
    bootstrap.application.bootstrap
    val v = Array("C", "B", "@", "A", "B", "E", "@")
    val s = Sorting.sortedArray(v, Ordering[String])
    println(v.toList)
    println(s.toArray.toList)
    println(s.toArray.map(v(_)).toList)
    assert(true)
  }

  private def testOrder[A](a: Array[Int], v: Array[A], ordering: Ordering[A]) = {
    import ordering._
    for (i ← 0 until (a.length - 1)) if (v(a(i)) > v(a(i + 1))) throw new Exception("incorrect " + i)
  }

  @Test def test2 = {
    val n = 1000000
    val v = Array.fill(n) { Random.nextInt(1000000000).toString }
    println(v.length)
    for (i ← 1 to 10) {
      println(time.timeMillis {
        val a = Sorting.sortedArray(v, Ordering[String])
      })
    }
    assert(true)
  }

  import java.sql.Timestamp

  @Test def test3 = {
    val n = 1000000
    val v = Array.fill(n) { Random.nextInt(1000000000).toLong }
    println(v.length)
    for (i ← 1 to 300) {
      println(time.timeMillis {
        Sorting.sortedArray(v, Ordering[Long])
        null
      })
    }
    assert(true)
  }

  @Test def test4 = {
    import java.sql.Timestamp
    val n = 5000000
    for (i ← 1 to 1) {
      val v = Array.fill(n) { new Timestamp(Random.nextInt(1000000000)) }
      println(time.timeMillis {
        java.util.Arrays.sort(v, new Ordering[Timestamp] {
          final def compare(a: Timestamp, b: Timestamp) = Ordering[Long].compare(a.getTime, b.getTime)
        })
        null
      })
    }
    assert(true)
  }

}