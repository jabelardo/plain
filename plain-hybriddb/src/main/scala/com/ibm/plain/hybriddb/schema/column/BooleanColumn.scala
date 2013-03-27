package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
object BooleanColumn {

  type BitSet = scala.collection.mutable.BitSet

}

import BooleanColumn.BitSet

/**
 *
 */
final class BooleanColumn(

  val length: IndexType,

  private[this] final val trues: BitSet,

  private[this] final val falses: BitSet)

  extends Column[Boolean]

  with Lookup[Boolean] {

  final def get(index: IndexType) = trues.contains(index)

  final def lookup(value: Boolean): IndexIterator = if (value) trues.iterator else falses.iterator

}

/**
 *
 */
final class BooleanColumnBuilder(

  capacity: IndexType)

  extends ColumnBuilder[Boolean, BooleanColumn] {

  final def set(index: IndexType, value: Boolean) = if (value) trues.add(index) else falses.add(index)

  final def get = new BooleanColumn(trues.size + falses.size, trues, falses)

  private[this] final val trues = new BitSet(capacity)

  private[this] final val falses = new BitSet(capacity)

}

