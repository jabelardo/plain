package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

/**
 *
 */
object CompressedColumn {

  type KeyMap[A] = scala.collection.mutable.OpenHashMap[A, Set[IndexType]]

}

import CompressedColumn._

/**
 *
 */
final class CompressedColumn[A](

  val length: IndexType,

  private[this] final val keys: KeyMap[A],

  private[this] final val values: Array[IndexType],

  private[this] final val distinctvalues: Array[A])

  extends Column[A]

  with Lookup[A] {

  final def get(index: IndexType): A = distinctvalues(values(index))

  final def lookup(value: A): LookupSetType = keys.get(value) match {
    case Some(s) ⇒ s
    case _ ⇒ Set.empty
  }

}

/**
 *
 */
final class CompressedColumnBuilder[A: ClassTag](

  capacity: IndexType)

  extends ColumnBuilder[A, CompressedColumn[A]] {

  final def set(index: IndexType, value: A): Unit = keys.put(value, keys.getOrElse(value, Set.empty) + index)

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
    new CompressedColumn[A](length, keys, values, distinctvalues)
  }

  private[this] final val keys = new KeyMap[A](capacity / 1000)

  private[this] final val values = new Array[IndexType](capacity)

}

