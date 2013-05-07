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
@SerialVersionUID(1L) final class IndexedColumn[A: ClassTag, -O <: Ordering[A]](

  val length: Long,

  protected[this] final val values: IndexedSeq[A],

  protected[this] final val ordering: O)

  extends BuiltColumn[A]

  with BaseIndexed[A] {

  type Builder = IndexedColumnBuilder[A]

  @inline final def get(index: Long): A = values(index.toInt)

  protected[this] final val array = sortedIndexedSeq(values, ordering)

}

/**
 *
 */
final class IndexedColumnBuilder[A: ClassTag](

  val capacity: Long,

  ordering: Ordering[A])

  extends ColumnBuilder[A, IndexedColumn[A, Ordering[A]]] {

  final def next(value: A): Unit = array.update(nextIndex.toInt, value)

  def result = new IndexedColumn(length, array, ordering)

  protected[this] final val array = new Array[A](capacity.toInt)

}

