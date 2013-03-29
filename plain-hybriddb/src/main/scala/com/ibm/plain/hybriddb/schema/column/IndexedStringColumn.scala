package com.ibm

package plain

package hybriddb

package schema

package column

import collection.immutable.Sorting.binarySearch

/**
 *
 */
final class IndexedStringColumn(

  length: IndexType,

  values: Array[String],

  ordering: Ordering[String])

  extends IndexedColumn[String](length, values, ordering) {

  final def startsWith(value: String): IndexIterator = new IndexIterator {

    @inline final def hasNext = lower < array.length && values(array(lower)).startsWith(value)

    @inline final def next = { lower += 1; array(lower - 1) }

    private[this] final var lower = binarySearch(value, array, values, (a: String, b: String) ⇒ a >= b).getOrElse(Int.MaxValue)

  }

  final def startsWith(value: String, ignorecase: Boolean): IndexIterator =
    if (ignorecase)
      new IndexIterator {

        @inline final def hasNext = lower < array.length && values(array(lower)).toLowerCase.startsWith(value.toLowerCase)

        @inline final def next = { lower += 1; array(lower - 1) }

        private[this] final var lower = binarySearch(value, array, values, (a: String, b: String) ⇒ a.toLowerCase >= b.toLowerCase).getOrElse(Int.MaxValue)

      }
    else
      startsWith(value)

  /**
   * The .view makes all the difference here.
   */
  final def contains(value: String): IndexIterator = array.view.filter(i ⇒ values(i).contains(value)).iterator

  final def contains(value: String, ignorecase: Boolean): IndexIterator =
    if (ignorecase)
      array.view.filter(i ⇒ values(i).toLowerCase.contains(value.toLowerCase)).iterator
    else
      contains(value)

  final def matches(regex: String): IndexIterator = array.view.filter(i ⇒ values(i).matches(regex)).iterator

}

