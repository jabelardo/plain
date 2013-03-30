package com.ibm

package plain

package hybriddb

package schema

package column

import collection.immutable.Sorting.{ binarySearch, sortedArray }

/**
 *
 */
class IndexedColumn[A](

  length: IndexType,

  values: Array[A],

  ordering: Ordering[A])

  extends ArrayColumn[A](length, values)

  with Indexed[A] {

  import ordering._

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

  protected[this] final val array = sortedArray(values, ordering)

  private[this] final class Iter(

    private[this] final var lower: IndexType,

    private[this] final val upper: IndexType)

    extends IndexIterator {

    @inline final def hasNext = lower < upper

    @inline final def next = { lower += 1; array(lower - 1) }

  }

}

