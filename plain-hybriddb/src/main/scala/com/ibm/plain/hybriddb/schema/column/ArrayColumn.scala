package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

/**
 *
 */
class ArrayColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  val length: IndexType,

  private[this] final val array: Array[A])

  extends Column[A] {

  @inline final def get(index: IndexType): A = array(index)

  require(length <= array.length)

}

/**
 *
 */
final class ArrayColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  capacity: IndexType)

  extends ColumnBuilder[A, ArrayColumn[A]] {

  final def set(index: IndexType, value: A): Unit = {
    if (index >= length) length = index + 1
    array.update(index, value)
  }

  final def get = new ArrayColumn[A](length, array)

  private[this] final val array = new Array[A](capacity)

  private[this] final var length: IndexType = 0

}

