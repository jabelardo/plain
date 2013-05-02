package com.ibm

package plain

package hybriddb

package schema

package column

import scala.math.max
import scala.reflect.ClassTag

/**
 *
 */
object DictionaryColumn {

  type IntSet = scala.collection.mutable.HashSet[Int]

  type KeyMap[A] = scala.collection.mutable.OpenHashMap[A, IntSet]

}

import DictionaryColumn._

/**
 * Use this for columns with few distinct values compared to their length (< 5%).
 */
@SerialVersionUID(1L) final class DictionaryColumn[@specialized A](

  val length: Long,

  private[this] final val keys: Map[A, IntSet],

  private[this] final val values: Array[Int],

  private[this] final val distinctvalues: Array[A])

  extends BuiltColumn[A]

  with Lookup[A] {

  type Builder = DictionaryColumnBuilder[A]

  final def get(index: Long): A = distinctvalues(values(index.toInt))

  final def lookup(value: A): Iterator[Long] = keys.get(value) match {
    case Some(s) ⇒ s.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final class DictionaryColumnBuilder[@specialized A: ClassTag](

  val capacity: Long)

  extends ColumnBuilder[A, DictionaryColumn[A]] {

  final def next(value: A): Unit = keys.put(value, keys.getOrElse(value, new IntSet) += nextIndex.toInt)

  final def result = {
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
    new DictionaryColumn[A](length, keys.toMap, values, distinctvalues)
  }

  private[this] final val keys = new KeyMap[A](max(capacity.toInt, 16))

}

