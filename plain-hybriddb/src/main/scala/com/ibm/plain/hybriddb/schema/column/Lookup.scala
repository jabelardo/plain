package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Lookup[@specialized A] {

  def lookup(value: A): Iterator[Long]

}

