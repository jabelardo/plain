package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Unique[A] {

  def unique(value: A): Option[IndexType]

  final def apply(value: A) = unique(value)

}

