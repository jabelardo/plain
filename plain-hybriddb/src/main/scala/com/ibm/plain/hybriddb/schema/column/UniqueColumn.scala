package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

/**
 *
 */
object UniqueColumn {

  type UniqueMap[A] = scala.collection.mutable.OpenHashMap[A, Long]

}

import UniqueColumn._

/**
 *
 */
final class UniqueColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  val length: Long,

  private[this] final val array: Array[A],

  private[this] final val keys: UniqueMap[A])

  extends BuiltColumn[A]

  with Unique[A] {

  type Builder = UniqueColumnBuilder[A]

  @inline final def get(index: Long): A = array(index.toInt)

  final def unique(value: A): Option[Long] = keys.get(value) match { case None ⇒ None case i ⇒ i }

  require(length <= array.length)

}

/**
 *
 */
final class UniqueColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  val capacity: Long)

  extends ColumnBuilder[A, UniqueColumn[A]] {

  final def next(value: A): Unit = {
    val i = nextIndex.toInt
    array.update(i, value)
    keys.put(value, i)
  }

  final def result = new UniqueColumn[A](keys.size, array, keys)

  private[this] final val keys = new UniqueMap[A](capacity.toInt)

  private[this] final val array = new Array[A](capacity.toInt)

}

