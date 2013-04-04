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

  length: IndexType,

  protected final val values: IndexedSeq[A],

  protected final val ordering: Ordering[A])

  extends ArrayColumn[A](name, length, values.toArray)

  with BaseIndexed[A] {

  protected final val array = sortedIndexedSeq(values, ordering)

}

/**
 *
 */
class IndexedColumnBuilder[A: ClassTag](

  name: String,

  capacity: IndexType,

  ordering: Ordering[A])

  extends ColumnBuilder[A, IndexedColumn[A]] {

  final def next(value: A): Unit = array.update(nextIndex, value)

  def get = new IndexedColumn[A](name, length, array, ordering)

  protected[this] final val array = new Array[A](capacity)

}





