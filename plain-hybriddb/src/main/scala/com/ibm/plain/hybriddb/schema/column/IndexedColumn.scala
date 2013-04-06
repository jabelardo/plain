package com.ibm

package plain

package hybriddb

package schema

package column

import scala.collection.IndexedSeq
import scala.reflect.ClassTag

import collection.immutable.Sorting.{ binarySearch, sortedIndexedSeq }

/**
 *
 */
class IndexedColumn[A: ClassTag] private[column] (

  name: String,

  length: Long,

  protected final val values: IndexedSeq[A],

  protected final val ordering: Ordering[A])

  extends ArrayColumn[A](name, length, values.toArray)

  with BaseIndexed[A] {

  protected final val array = sortedIndexedSeq(values, ordering)

}

/**
 *
 */
class IndexedColumnBuilder[A: ClassTag] protected (

  val name: String,

  val capacity: Long,

  ordering: Ordering[A])

  extends ColumnBuilder[A, IndexedColumn[A]] {

  final def next(value: A): Unit = array.update(nextIndex.toInt, value)

  def result = new IndexedColumn[A](name, length, array, ordering)

  protected[this] final val array = new Array[A](capacity.toInt)

}

/**
 *
 */
object IndexedColumnBuilder {

  def apply[A: ClassTag](name: String, capacity: Long, ordering: Ordering[A]) = new IndexedColumnBuilder[A](name, capacity, ordering)

}

