package com.ibm

package plain

package hybriddb

package schema

package column

import scala.collection.IndexedSeq

import collection.immutable.Sorting.binarySearch

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
 *
 */
trait StringIndexed {

  def startsWith(value: String): Iterator[Long]

}

/**
 * Implements most methods of Indexed based on an IndexedSeq.
 */
trait BaseIndexed[A]

  extends Indexed[A] {

  protected[this] val ordering: Ordering[A]

  import ordering._

  protected[this] def values: IndexedSeq[A]

  protected[this] def array: IndexedSeq[Int]

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

/**
 *
 */
trait BaseStringIndexed

  extends StringIndexed {

  protected[this] val ordering: Ordering[String]

  protected[this] def values: IndexedSeq[String]

  protected[this] def array: IndexedSeq[Int]

  /**
   * Still very fast as it is using binarySearch to find the first value.
   */
  final def startsWith(value: String): Iterator[Long] = new Iterator[Long] {

    @inline final def hasNext = lower < array.length && values(array(lower)).startsWith(value)

    @inline final def next = { lower += 1; array(lower - 1) }

    private[this] final var lower = binarySearch(value, array, values, (a: String, b: String) ⇒ a >= b).getOrElse(Int.MaxValue)

  }

  final def startsWith(value: String, ignorecase: Boolean): Iterator[Long] = {
    if (ignorecase) new Iterator[Long] {

      @inline final def hasNext = lower < array.length && values(array(lower)).toLowerCase.startsWith(value.toLowerCase)

      @inline final def next = { lower += 1; array(lower - 1) }

      private[this] final var lower = binarySearch(value, array, values, (a: String, b: String) ⇒ a.toLowerCase >= b.toLowerCase).getOrElse(Int.MaxValue)

    }
    else startsWith(value)
  }

  /**
   * The .view makes all the difference here.
   */
  final def contains(value: String): Iterator[Long] = array.view.filter(i ⇒ values(i).contains(value)).iterator

  /**
   * The .view makes all the difference here.
   */
  final def contains(value: String, ignorecase: Boolean): Iterator[Long] = {
    if (ignorecase)
      array.view.filter(i ⇒ values(i).toLowerCase.contains(value.toLowerCase)).iterator
    else
      contains(value)
  }

  /**
   * The .view makes all the difference here.
   */
  final def matches(regex: String): Iterator[Long] = array.view.filter(i ⇒ values(i).matches(regex)).iterator

}

