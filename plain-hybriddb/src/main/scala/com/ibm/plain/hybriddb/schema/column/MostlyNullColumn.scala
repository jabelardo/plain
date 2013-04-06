package com.ibm

package plain

package hybriddb

package schema

package column

import scala.reflect.ClassTag

import reflect.mirror

/**
 *
 */
object MostlyNullColumn {

  type IntSet = scala.collection.mutable.HashSet[Int]

  type KeyMap[A] = scala.collection.mutable.OpenHashMap[A, IntSet]

  type BitSet = scala.collection.mutable.BitSet

}

import MostlyNullColumn._

/**
 *
 */
final class MostlyNullColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  val name: String,

  val length: Long,

  private[this] final val keys: KeyMap[A],

  private[this] final val values: Array[Int],

  private[this] final val distinctvalues: Array[A],

  private[this] final val nulls: BitSet)

  extends Column[Option[A]]

  with Lookup[Option[A]] {

  final def get(index: Long): Option[A] = if (nulls.contains(index.toInt)) None else Some(distinctvalues(values(index.toInt)))

  final def lookup(value: Option[A]): Iterator[Long] = if (value.isEmpty) nulls.iterator else keys.get(value.get) match {
    case Some(s) ⇒ s.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final case class MostlyNullColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A: ClassTag](

  val name: String,

  val capacity: Long)

  extends ColumnBuilder[Option[A], MostlyNullColumn[A]] {

  final def next(value: Option[A]): Unit = if (value.isEmpty) nulls.add(nextIndex.toInt) else keys.put(value.get, keys.getOrElse(value.get, new IntSet) += nextIndex.toInt)

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
    new MostlyNullColumn[A](name, length, keys, values, distinctvalues, nulls)
  }

  private[this] final val keys = new KeyMap[A](capacity.toInt / 1000)

  private[this] final val nulls = new BitSet(capacity.toInt / 3)

}
