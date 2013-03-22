package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Lookup[A] {

  type LookupSetType = scala.collection.Set[IndexType]

  def lookup(value: A): LookupSetType

}

