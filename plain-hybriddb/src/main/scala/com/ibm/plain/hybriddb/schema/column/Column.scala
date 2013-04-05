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

  def name: String

  def length: Long

  def get(index: Long): A

  final def apply(index: Long) = get(index)

}

/**
 *
 */
trait ColumnBuilder[A, C <: Column[_]] {

  def get: C

  def next(value: A)

  final def apply(index: Long, value: A) = next(value)

  protected[this] def length: Long = index

  protected[this] def nextIndex: Long = { index += 1L; index - 1L }

  private[this] final var index: Long = 0

}
