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

  val name: String,

  val length: Long,

  private[this] final val array: Array[A])

  extends Column[A] {

  @inline final def get(index: Long): A = array(index.toInt)

  require(length <= array.length)

}

/**
 *
 */
final case class ArrayColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  val name: String,

  val capacity: Long)

  extends ColumnBuilder[A, ArrayColumn[A]] {

  final def next(value: A): Unit = array.update(nextIndex.toInt, value)

  final def result = new ArrayColumn[A](name, length, array)

  private[this] final val array = new Array[A](capacity.toInt)

}

