package com.ibm

package plain

package collection

package immutable

import scala.concurrent.{ Await, Future, TimeoutException }
import scala.concurrent.duration.Duration
import scala.annotation.tailrec
import scala.collection.IndexedSeq

import concurrent.{ future, parallelism }

/**
 * see: scala.util.Sorting.scala
 */
object Sorting {

  final def sortedIndexedSeq[A](
    values: IndexedSeq[A],
    ordering: Ordering[A]): IndexedSeq[Int] = {
    val array = Array.range(0, values.length)
    quickSort(array, values, ordering)
    array
  }

  /**
   * Sorts an array of indices (Array[Int]) according to a given Array[A] and an Ordering[A],
   * uses ForkJoinPool to do sorting in parallel (3x faster on 8-cpu-system).
   */
  final def quickSort[A](
    array: Array[Int],
    off: Int,
    len: Int,
    values: IndexedSeq[A],
    ordering: Ordering[A]) = {

    import ordering._

    @inline def swap(a: Int, b: Int) {
      val t = array(a)
      array(a) = array(b)
      array(b) = t
    }

    @inline def vecswap(_a: Int, _b: Int, n: Int) {
      var a = _a
      var b = _b
      var i = 0
      while (i < n) {
        swap(a, b)
        i += 1
        a += 1
        b += 1
      }
    }

    @inline def med3(a: Int, b: Int, c: Int) = {
      if (values(array(a)) < values(array(b))) {
        if (values(array(b)) < values(array(c))) b else if (values(array(a)) < values(array(c))) c else a
      } else {
        if (values(array(b)) > values(array(c))) b else if (values(array(a)) > values(array(c))) c else a
      }
    }

    def sort2(off: Int, len: Int) {
      // Insertion sort on smallest arrays
      if (len < 7) {
        var i = off
        while (i < len + off) {
          var j = i
          while (j > off && values(array(j - 1)) > values(array(j))) {
            swap(j, j - 1)
            j -= 1
          }
          i += 1
        }
      } else {
        // Choose a partition element, v
        var m = off + (len >> 1) // Small arrays, middle element
        if (len > 7) {
          var l = off
          var n = off + len - 1
          if (len > 40) { // Big arrays, pseudomedian of 9
            val s = len / 8
            l = med3(l, l + s, l + 2 * s)
            m = med3(m - s, m, m + s)
            n = med3(n - 2 * s, n - s, n)
          }
          m = med3(l, m, n) // Mid-size, med of 3
        }
        val v = values(array(m))

        // Establish Invariant: v* (<v)* (>v)* v*
        var a = off
        var b = a
        var c = off + len - 1
        var d = c
        var done = false
        while (!done) {
          while (b <= c && values(array(b)) <= v) {
            if (values(array(b)) == v) {
              swap(a, b)
              a += 1
            }
            b += 1
          }
          while (c >= b && values(array(c)) >= v) {
            if (values(array(c)) == v) {
              swap(c, d)
              d -= 1
            }
            c -= 1
          }
          if (b > c) {
            done = true
          } else {
            swap(b, c)
            c -= 1
            b += 1
          }
        }

        // Swap partition elements back to middle
        val n = off + len
        var s = scala.math.min(a - off, b - a)
        vecswap(off, b - s, s)
        s = scala.math.min(d - c, n - d - 1)
        vecswap(b, n - s, s)

        // Recursively sort non-partition-elements
        var f: Future[Unit] = null

        val ls = b - a
        if (ls > 1) if (ls > (array.length / parallelism)) f = future { sort2(off, ls) } else (sort2(off, ls))

        val rs = d - c
        if (rs > 1) sort2(n - rs, rs)

        if (null != f) Await.ready(f, Duration.Inf)
      }
    }
    sort2(off, len)
  }

  final def quickSort[A](
    array: Array[Int],
    values: IndexedSeq[A],
    ordering: Ordering[A]): Unit = quickSort(array, 0, array.length, values, ordering)

  /**
   * Best suited to implement a values.contains(value) - which already exists, not that useful, really.
   */
  final def recursiveBinarySearch[A](
    value: A,
    array: Array[Int],
    offset: Int,
    length: Int,
    values: IndexedSeq[A],
    ordering: Ordering[A]): Option[Int] = {
    @tailrec def recurse(low: Int, high: Int): Option[Int] = {
      if (high < low) None else {
        val mid = (low + high) >>> 1
        ordering.compare(values(array(mid)), value) match {
          case 0          ⇒ Some(mid)
          case c if 0 < c ⇒ recurse(low, mid - 1)
          case c if 0 > c ⇒ recurse(mid + 1, high)
        }
      }
    }
    recurse(offset, offset + length)
  }

  /**
   * Better than the above, a one-comparison-per-iteration version of binary search, it always returns the "first of" element.
   * Must be called with an operator of type > or >=. All other should be deducted.
   */
  final def binarySearch[A](
    value: A,
    array: IndexedSeq[Int],
    offset: Int,
    length: Int,
    values: IndexedSeq[A],
    op: (A, A) ⇒ Boolean): Option[Int] = {
    var low = offset
    var high = offset + length
    while (high > low) {
      val mid = low + ((high - low) / 2)
      if (op(values(array(mid)), value)) {
        high = mid
      } else {
        low = mid + 1
      }
    }
    if (low == high) Some(low) else None
  }

  final def binarySearch[A](
    value: A,
    array: IndexedSeq[Int],
    values: IndexedSeq[A],
    op: (A, A) ⇒ Boolean): Option[Int] = binarySearch(value, array, 0, array.length, values, op)

}
