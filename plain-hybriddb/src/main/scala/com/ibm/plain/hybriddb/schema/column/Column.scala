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

  override def toString = getClass.getSimpleName + "(name=" + name + ")"

}

/**
 *
 */
trait ColumnBuilder[A, C <: Column[_]] {

  def name: String

  def capacity: Long

  def length: Long = index

  def result: C

  def next(value: A)

  def nextAny(value: Any): Unit = next(value.asInstanceOf[A])

  final def apply(value: A) = next(value)

  override def toString = getClass.getSimpleName + "(name=" + name + " capacity=" + capacity + ")"

  protected[this] final def nextIndex: Long = { index += 1L; index - 1L }

  private[this] final var index: Long = 0

}

