package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Indexed[A] {

  def equiv(value: A): IndexIterator

  def lt(value: A): IndexIterator

  def gt(value: A): IndexIterator

  def lteq(value: A): IndexIterator

  def gteq(value: A): IndexIterator

  def between(low: A, high: A): IndexIterator

}

