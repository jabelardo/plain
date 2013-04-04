package com.ibm

package plain

package collection

package immutable

import org.junit.Assert.assertTrue
import org.junit.Test
import scala.util.Random
import scala.math.Ordering._

// add @Test for testing

final class TestSortedArray {

  @Test def test1 = {
    bootstrap.application.bootstrap
    val v = Array("C", "B", "@", "A", "B", "E", "@")
    val s = Sorting.sortedIndexedSeq(v, Ordering[String])
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
    println(v.toList.take(50))
    for (i ← 1 to 1) {
      println(time.timeMillis {
        val a = Sorting.sortedIndexedSeq(v, Ordering[String])
        val o = new Ordering[String] {
          def compare(a: String, b: String): Int = {
            println(a + " " + b)
            if (a.startsWith(b)) 0 else Ordering[String].compare(a, b)
          }
        }
        var p = Sorting.recursiveBinarySearch[String]("77", a.toArray, 0, a.length, v, o).get
        while (v(a(p)).startsWith("77")) { println(p + " " + v(a(p))); p += 1 }
      })
    }
    assert(true)
  }

  import java.sql.Timestamp

  @Test def test3 = {
    val n = 50000000
    val v = Array.fill(n) { Random.nextInt(1000000000).toLong }
    val x = 47114711
    v(333) = x
    println(v.length)
    var t = 0L
    val m = 1000000
    val (s, st) = time.timeNanos(Sorting.sortedIndexedSeq(v, Ordering[Long]))
    println(s.length + " " + " t + " + st)
    for (i ← 1 to m) {
      t += time.timeNanos(Sorting.binarySearch[Long](x, s, v, (a: Long, b: Long) ⇒ a > b))._2
    }
    println("average " + (t / m))
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