package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Column[A]

  extends Serializable {

  type ColumnType = A

  def length: IndexType

  def get(index: IndexType): A

  final def apply(index: IndexType) = get(index)

}

/**
 *
 */
trait ColumnBuilder[A, C <: Column[_]] {

  def get: C

  def next(value: A)

  final def apply(index: IndexType, value: A) = next(value)

  protected[this] def length: IndexType = index

  protected[this] def nextIndex: IndexType = { index += 1; index - 1 }

  private[this] final var index: IndexType = 0

}
