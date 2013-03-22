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

  type SelfType = this.type

  def length: IndexType

  def get(index: IndexType): A

  @inline final def apply(index: IndexType) = get(index)

}

/**
 *
 */
trait ColumnBuilder[A, C <: Column[_]] {

  def get: C

  def set(index: IndexType, value: A)

  @inline final def apply(index: IndexType, value: A) = set(index, value)

}
