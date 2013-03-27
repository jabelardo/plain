package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Indexed[A]

  extends Lookup[A] {

  @inline final def ==(value: A): IndexIterator = lookup(value)

  def <(value: A): IndexIterator

  def <=(value: A): IndexIterator

  def >(value: A): IndexIterator

  def >=(value: A): IndexIterator

  def between(lower: A, upper: A): IndexIterator

}

