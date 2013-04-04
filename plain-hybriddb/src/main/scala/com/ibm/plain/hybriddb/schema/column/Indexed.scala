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

  def equiv(value: A): IndexIterator

  def lt(value: A): IndexIterator

  def gt(value: A): IndexIterator

  def lteq(value: A): IndexIterator

  def gteq(value: A): IndexIterator

  def between(low: A, high: A): IndexIterator

}

/**
 * Implements most methods of Indexed based on an IndexedSeq.
 */
trait BaseIndexed[A]

  extends Indexed[A] {

  protected val ordering: Ordering[A]

  import ordering._

  protected def values: IndexedSeq[A]

  protected def array: IndexedSeq[IndexType]

  final def equiv(value: A): IndexIterator = new IndexIterator {

    @inline final def hasNext = lower < array.length && value == values(array(lower))

    @inline final def next = { lower += 1; array(lower - 1) }

    private[this] final var lower = binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue)

  }

  final def gt(value: A): IndexIterator = new Iter(binarySearch(value, array, values, (a: A, b: A) ⇒ a > b).getOrElse(Int.MaxValue), array.length)

  final def gteq(value: A): IndexIterator = new Iter(binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue), array.length)

  final def lt(value: A): IndexIterator = new Iter(0, binarySearch(value, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(-1))

  final def lteq(value: A): IndexIterator = new Iter(0, binarySearch(value, array, values, (a: A, b: A) ⇒ a > b).getOrElse(-1))

  final def between(low: A, high: A): IndexIterator = new Iter(
    binarySearch(low, array, values, (a: A, b: A) ⇒ a >= b).getOrElse(Int.MaxValue),
    binarySearch(high, array, values, (a: A, b: A) ⇒ a > b).getOrElse(0) - 1)

  private[this] final class Iter(

    private[this] final var lower: IndexType,

    private[this] final val upper: IndexType)

    extends IndexIterator {

    @inline final def hasNext = lower < upper

    @inline final def next = { lower += 1; array(lower - 1) }

  }

}

