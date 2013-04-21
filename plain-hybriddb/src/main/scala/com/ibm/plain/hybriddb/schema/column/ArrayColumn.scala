package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

/**
 *
 */
final class ArrayColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  final val length: Long,

  private[this] final val array: Array[A])

  extends BuiltColumn[A] {

  type Builder = ArrayColumnBuilder[A]

  @inline final def get(index: Long): A = array(index.toInt)

  require(length <= array.length)

}

/**
 *
 */
final class ArrayColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  val capacity: Long)

  extends ColumnBuilder[A, ArrayColumn[A]] {

  final def next(value: A): Unit = array.update(nextIndex.toInt, value)

  final def result = new ArrayColumn[A](length, array)

  private[this] final val array = new Array[A](capacity.toInt)

}

