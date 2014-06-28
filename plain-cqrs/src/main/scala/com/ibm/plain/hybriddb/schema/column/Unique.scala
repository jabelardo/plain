package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Unique[@specialized A] {

  def unique(value: A): Option[Long]

}

