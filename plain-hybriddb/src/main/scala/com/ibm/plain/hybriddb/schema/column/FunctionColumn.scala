package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
final class FunctionColumn[A](

  val length: IndexType,

  private[this] final val f: IndexType ⇒ A)

  extends Column[A] {

  @inline final def get(index: IndexType): A = f(index)

}
