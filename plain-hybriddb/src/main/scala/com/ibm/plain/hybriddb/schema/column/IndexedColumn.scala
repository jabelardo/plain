package com.ibm

package plain

package hybriddb

package schema

package column

import collection.immutable.Sorting._

/**
 *
 */
class IndexedColumn[A](

  length: IndexType,

  values: Array[A],

  ordering: Ordering[A])

  extends ArrayColumn[A](length, values)

  with Indexed[A] {

  final def <(value: A): IndexIterator = new Iter(0, binarySearch(value, array, values, ordering).getOrElse(-1))

  final def <=(value: A): IndexIterator = new Iter(0, binarySearch(value, array, values, ordering).getOrElse(-2) + 1)

  final def >(value: A): IndexIterator = new Iter(binarySearch(value, array, values, ordering).getOrElse(Int.MaxValue - 1) + 1, array.length)

  final def >=(value: A): IndexIterator = new Iter(binarySearch(value, array, values, ordering).getOrElse(Int.MaxValue), array.length)

  final def between(lower: A, upper: A): IndexIterator = new Iter(
    binarySearch(lower, array, values, ordering).getOrElse(Int.MaxValue),
    binarySearch(lower, array, values, ordering).getOrElse(-2) + 1)

  final def lookup(value: A): IndexIterator = new IndexIterator {

    final def hasNext = i < array.length && value == array(i)

    final def next = { i += 1; array(i - 1) }

    private[this] final var i = binarySearch(value, array, values, ordering).getOrElse(-1)

  }

  protected[this] final val array = sortedArray(values, ordering)

  private[this] final class Iter(

    final var i: IndexType,

    final val upper: IndexType)

    extends IndexIterator {

    final def hasNext = i < upper

    final def next = { i += 1; array(i - 1) }

  }

}


