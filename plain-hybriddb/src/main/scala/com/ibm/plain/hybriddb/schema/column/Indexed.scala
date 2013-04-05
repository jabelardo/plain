package com.ibm

package plain

package hybriddb

package schema

package column

import scala.collection.IndexedSeq

import collection.immutable.Sorting.{ binarySearch, sortedIndexedSeq }

/**
 *
 */
trait Indexed[A] {

  def equiv(value: A): Iterator[Long]

  def lt(value: A): Iterator[Long]

  def gt(value: A): Iterator[Long]

  def lteq(value: A): Iterator[Long]

  def gteq(value: A): Iterator[Long]

  def between(low: A, high: A): Iterator[Long]

}

/**
 * Implements most methods of Indexed based on an IndexedSeq.
 */
trait BaseIndexed[A]

  extends Indexed[A] {

  protected val ordering: Ordering[A]

  import ordering._

  protected def values: IndexedSeq[A]

  protected def array: IndexedSeq[Int]

  final def equiv(value: A): Iterator[Long] = new Iterator[Long] {

    @inline final def hasNext = lower < array.length && value == values(array(lower))

    @inline final def next = { lower += 1; array(lower - 1) }

    private[this] final var lower = binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue)

  }

  final def gt(value: A): Iterator[Long] = new Iter(binarySearch(value, array, values, (a: A, b: A) ⇒ a > b).getOrElse(Int.MaxValue), array.length)

  final def gteq(value: A): Iterator[Long] = new Iter(binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue), array.length)

  final def lt(value: A): Iterator[Long] = new Iter(0, binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(-1))

  final def lteq(value: A): Iterator[Long] = new Iter(0, binarySearch(value, array, values, (a: A, b: A) ⇒ a > b).getOrElse(-1))

  final def between(low: A, high: A): Iterator[Long] = new Iter(
    binarySearch(low, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue),
    binarySearch(high, array, values, (a: A, b: A) ⇒ a > b).getOrElse(0) - 1)

  private[this] final class Iter(

    private[this] final var lower: Int,

    private[this] final val upper: Int)

    extends Iterator[Long] {

    @inline final def hasNext = lower < upper

    @inline final def next = { lower += 1; array(lower - 1) }

  }

}

