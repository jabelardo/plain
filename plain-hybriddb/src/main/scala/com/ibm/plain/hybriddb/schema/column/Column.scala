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

  type Builder <: ColumnBuilder[A, _]

  def length: Long

  def get(index: Long): A

  final def apply(index: Long) = get(index)

}

/**
 *
 */
trait BuiltColumn[A]

  extends Column[A] {

  type Builder <: ColumnBuilder[A, _]

}

/**
 *
 */
trait ColumnBuilder[A, C <: BuiltColumn[A]] {

  def capacity: Long

  def result: C

  def set(any: Any) = next(any.asInstanceOf[A])

  def next(value: A)

  final def apply(value: A) = next(value)

  final def length: Long = index

  protected[this] final def nextIndex: Long = { index += 1L; index - 1L }

  private[this] final var index: Long = 0

}


