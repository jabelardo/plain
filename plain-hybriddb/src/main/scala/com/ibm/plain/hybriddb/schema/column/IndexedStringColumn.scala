package com.ibm

package plain

package hybriddb

package schema

package column

import collection.immutable.Sorting.binarySearch

/**
 *
 */
final class IndexedStringColumn private[column] (

  name: String,

  length: Long,

  values: Array[String],

  ordering: Ordering[String])

  extends IndexedColumn[String](name, length, values, ordering) {

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

/**
 *
 */
final class IndexedStringColumnBuilder private (

  override val name: String,

  override val capacity: Long,

  ordering: Ordering[String])

  extends IndexedColumnBuilder[String](name, capacity, ordering) {

  override final def result = new IndexedStringColumn(name, length, array, ordering)

}

/**
 *
 */
object IndexedStringColumnBuilder {

  def apply(name: String, capacity: Long, ordering: Ordering[String]) = new IndexedStringColumnBuilder(name, capacity, ordering)

}


  