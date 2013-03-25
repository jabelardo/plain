package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
trait Sorted[A] {

}

/**
 *
 */
trait SortedBuilder[A] {

  val length: IndexType

  private[this] final val sorted = { var i: IndexType = 0; val a = Array.fill(length)({ i += 1; i }); a }

}
