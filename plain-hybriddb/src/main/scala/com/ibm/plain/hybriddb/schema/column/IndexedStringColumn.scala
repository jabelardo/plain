package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
class IndexedStringColumn(

  length: IndexType,

  values: Array[String],

  ordering: Ordering[String])

  extends IndexedColumn[String](length, values, ordering) {

  /**
   * should be a regex, at the moment it is only "contains"
   */
  final def like(value: String): IndexIterator = new IndexIterator {

    final def hasNext = i < seq.length

    final def next = { i += 1; seq(i - 1) }

    private[this] final val seq = array.filter(i ⇒ values(array(i)).contains(value))

    private[this] final var i = if (seq.isEmpty) Int.MaxValue else 0

  }

  final def startsWith(value: String): IndexIterator = new IndexIterator {

    final def hasNext = i < array.length && values(array(i)).startsWith(value)

    final def next = { i += 1; array(i - 1) }

    private[this] final var i = array.find(i ⇒ values(array(i)).startsWith(value)).getOrElse(Int.MaxValue)

  }

}


