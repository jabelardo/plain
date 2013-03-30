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

  type IndexTypeSet = scala.collection.mutable.HashSet[Int]

  type KeyMap[A] = scala.collection.mutable.OpenHashMap[A, IndexTypeSet]

}

import CompactColumn._

/**
 * Use this for columns with few distinct values compared to their length (< 5%).
 */
final class CompactColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  val length: IndexType,

  private[this] final val keys: KeyMap[A],

  private[this] final val values: Array[IndexType],

  private[this] final val distinctvalues: Array[A])

  extends Column[A]

  with Lookup[A] {

  final def get(index: IndexType): A = distinctvalues(values(index))

  final def lookup(value: A): IndexIterator = keys.get(value) match {
    case Some(s) ⇒ s.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final class CompactColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  capacity: IndexType)

  extends ColumnBuilder[A, CompactColumn[A]] {

  final def set(index: IndexType, value: A): Unit = keys.put(value, keys.getOrElse(value, new IndexTypeSet) += index)

  final def get = {
    val length = keys.foldLeft(0) { case (s, (_, v)) ⇒ s + v.size }
    val values = {
      val v = new Array[IndexType](length)
      var i = 0
      keys.foreach {
        case (value, key) ⇒
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
    new CompactColumn[A](length, keys, values, distinctvalues)
  }

  private[this] final val keys = new KeyMap[A](capacity / 1000)

  private[this] final val values = new Array[IndexType](capacity)

}

