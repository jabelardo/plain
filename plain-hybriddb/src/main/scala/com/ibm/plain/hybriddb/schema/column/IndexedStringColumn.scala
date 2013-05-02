package com.ibm

package plain

package hybriddb

package schema

package column

import scala.collection.IndexedSeq
import scala.reflect.ClassTag

import collection.immutable.Sorting.sortedIndexedSeq

/**
 *
 */
@SerialVersionUID(1L) final class IndexedStringColumn[-O <: Ordering[String]](

  val length: Long,

  protected[this] final val values: IndexedSeq[String],

  protected[this] final val ordering: O)

  extends BuiltColumn[String]

  with BaseIndexed[String]

  with BaseStringIndexed {

  type Builder = IndexedStringColumnBuilder

  @inline final def get(index: Long): String = values(index.toInt)

  protected[this] final val array = sortedIndexedSeq(values, ordering)

}

/**
 *
 */
final class IndexedStringColumnBuilder(

  val capacity: Long,

  ordering: Ordering[String])

  extends ColumnBuilder[String, IndexedStringColumn[Ordering[String]]] {

  final def next(value: String): Unit = array.update(nextIndex.toInt, value)

  def result = new IndexedStringColumn(length, array, ordering)

  protected[this] final val array = new Array[String](capacity.toInt)

}



