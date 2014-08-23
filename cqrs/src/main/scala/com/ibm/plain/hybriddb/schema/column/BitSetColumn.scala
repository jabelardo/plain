package com.ibm

package plain

package hybriddb

package schema

package column

import scala.math.max

/**
 *
 */
object BitSetColumn {

  type BitSet = scala.collection.mutable.BitSet

  type BitSetMap[A] = scala.collection.concurrent.TrieMap[A, BitSet]

}

import BitSetColumn._

/**
 * Use this for columns with very few distinct values (< 10).
 */
@SerialVersionUID(1L) final class BitSetColumn[A](

  val length: Long,

  private[this] final val bitsets: Map[A, BitSet])

    extends BuiltColumn[A]

    with Lookup[A] {

  type Builder = BitSetColumnBuilder[A]

  final def get(index: Long): A = bitsets.find { case (v, b) ⇒ b.contains(index.toInt) } match {
    case Some((value, _)) ⇒ value
    case None ⇒ throw new IndexOutOfBoundsException(index.toString)
  }

  final def lookup(value: A): Iterator[Long] = bitsets.get(value) match {
    case Some(b) ⇒ b.iterator
    case _ ⇒ Set.empty.iterator
  }

}

/**
 *
 */
final class BitSetColumnBuilder[A](

  val capacity: Long)

    extends ColumnBuilder[A, BitSetColumn[A]] {

  final def next(value: A): Unit = {
    val bitset = bitsets.get(value) match {
      case Some(b) ⇒ b
      case _ ⇒ val b = new BitSet; bitsets.put(value, b); b
    }
    bitset.add(nextIndex.toInt)
  }

  final def result = new BitSetColumn[A](bitsets.foldLeft(0) { case (s, (_, b)) ⇒ s + b.size }, bitsets.toMap)

  private[this] final val bitsets = new BitSetMap[A]

}

