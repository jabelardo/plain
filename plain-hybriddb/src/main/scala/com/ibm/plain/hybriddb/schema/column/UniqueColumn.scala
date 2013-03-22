package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect._

/**
 *
 */
object UniqueColumn {

  type UniqueMap[A] = scala.collection.mutable.OpenHashMap[A, IndexType]

}

import UniqueColumn._

/**
 *
 */
final class UniqueColumn[@specialized(Int, Long) A](

  val length: IndexType,

  private[this] final val array: Array[A],

  private[this] final val keys: UniqueMap[A])

  extends Column[A]

  with Unique[A] {

  @inline final def get(index: IndexType): A = array(index)

  final def unique(value: A): Option[IndexType] = keys.get(value) match { case None ⇒ None case i ⇒ i }

  require(length <= array.length)

}

/**
 *
 */
final class UniqueColumnBuilder[@specialized(Int, Long) A: ClassTag](

  capacity: IndexType)

  extends ColumnBuilder[A, UniqueColumn[A]] {

  final def set(index: IndexType, value: A): Unit = {
    array.update(index, value)
    keys.put(value, index)
  }

  final def get = new UniqueColumn[A](keys.size, array, keys)

  private[this] final val keys = new UniqueMap[A](capacity)

  private[this] final val array = new Array[A](capacity)

}

