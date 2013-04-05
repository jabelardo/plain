package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Unique[A] {

  def unique(value: A): Option[Long]

  final def apply(value: A) = unique(value)

}

