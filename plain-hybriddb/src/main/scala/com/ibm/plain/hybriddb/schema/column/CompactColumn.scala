package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

/**
 *
 */
object CompactColumn {

  type IntSet = scala.collection.mutable.HashSet[Int]

  type KeyMap[A] = scala.collection.mutable.OpenHashMap[A, IntSet]

}

import CompactColumn._

/**
 * Use this for columns with few distinct values compared to their length (< 5%).
 */
final class CompactColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A] private[column] (

  val name: String,

  val length: Long,

  private[this] final val keys: KeyMap[A],

  private[this] final val values: Array[Int],

  private[this] final val distinctvalues: Array[A])

  extends Column[A]

  with Lookup[A] {

  final def get(index: Long): A = distinctvalues(values(index.toInt))

  final def lookup(value: A): Iterator[Long] = keys.get(value) match {
    case Some(s) ⇒ s.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final class CompactColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  name: String,

  capacity: Long)

  extends ColumnBuilder[A, CompactColumn[A]] {

  final def next(value: A): Unit = keys.put(value, keys.getOrElse(value, new IntSet) += nextIndex.toInt)

  final def get = {
    val length = keys.foldLeft(0) { case (s, (_, v)) ⇒ s + v.size }
    val values = {
      val v = new Array[Int](length)
      var i = 0
      keys.foreach {
        case (_, key) ⇒
          key.foreach(v.update(_, i))
          i += 1
      }
      v
    }
    val distinctvalues = {
      val d = new Array[A](keys.size)
      var i = 0
      keys.foreach {
        case (value, key) ⇒
          d.update(i, value)
          i += 1
      }
      d
    }
    new CompactColumn[A](name, length, keys, values, distinctvalues)
  }

  private[this] final val keys = new KeyMap[A](capacity.toInt / 1000)

}

