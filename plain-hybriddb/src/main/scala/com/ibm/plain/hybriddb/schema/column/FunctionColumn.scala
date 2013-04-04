package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
final class FunctionColumn[A] private (

  val name: String,

  val length: IndexType,

  private[this] final val f: IndexType ⇒ A)

  extends Column[A] {

  final def get(index: IndexType): A = f(index)

}

object FunctionColumn {

  def apply[A](name: String, length: IndexType, f: IndexType ⇒ A) = new FunctionColumn(name, length, f)

}
