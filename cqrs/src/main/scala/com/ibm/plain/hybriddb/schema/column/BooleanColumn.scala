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
@SerialVersionUID(1L) final class BooleanColumn(

  val length: Long,

  private[this] final val trues: BitSet,

  private[this] final val falses: BitSet)

    extends BuiltColumn[Boolean]

    with Lookup[Boolean] {

  type Builder = BooleanColumnBuilder

  final def get(index: Long) = trues.contains(index.toInt)

  final def lookup(value: Boolean): Iterator[Long] = if (value) trues.iterator else falses.iterator

}

/**
 *
 */
final class BooleanColumnBuilder(

  val capacity: Long)

    extends ColumnBuilder[Boolean, BooleanColumn] {

  final def next(value: Boolean) = if (value) trues.add(nextIndex.toInt) else falses.add(nextIndex.toInt)

  final def result = new BooleanColumn(trues.size + falses.size, trues, falses)

  private[this] final val trues = new BitSet(capacity.toInt)

  private[this] final val falses = new BitSet(capacity.toInt)

}
