package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
object BitSetColumn {

  type BitSet = scala.collection.mutable.BitSet

  type BitSetMap[A] = scala.collection.mutable.OpenHashMap[A, BitSet]

}

import BitSetColumn._

/**
 * Use this for columns with very few distinct values (< 10).
 */
final class BitSetColumn[@specialized(Byte, Char, Short, Int, Long, Float, Double) A] private[column] (

  val name: String,

  val length: IndexType,

  private[this] final val bitsets: BitSetMap[A])

  extends Column[A]

  with Lookup[A] {

  final def get(index: IndexType): A = bitsets.find { case (v, b) ⇒ b.contains(index) } match {
    case Some((value, _)) ⇒ value
    case None ⇒ throw new IndexOutOfBoundsException(index.toString)
  }

  final def lookup(value: A): IndexIterator = bitsets.get(value) match {
    case Some(b) ⇒ b.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final class BitSetColumnBuilder[@specialized(Byte, Char, Short, Int, Long, Float, Double) A](

  name: String,

  capacity: IndexType)

  extends ColumnBuilder[A, BitSetColumn[A]] {

  final def next(value: A): Unit = {
    val bitset = bitsets.get(value) match {
      case Some(b) ⇒ b
      case _ ⇒ val b = new BitSet(capacity / 16); bitsets.put(value, b); b
    }
    bitset.add(nextIndex)
  }

  final def get = new BitSetColumn[A](name, bitsets.foldLeft(0) { case (s, (_, b)) ⇒ s + b.size }, bitsets)

  private[this] final val bitsets = new BitSetMap[A](16)

}

