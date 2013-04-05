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

  val length: Long,

  private[this] final val f: Long ⇒ A)

  extends Column[A] {

  final def get(index: Long): A = f(index)

}

object FunctionColumn {

  def apply[A](name: String, length: Long, f: Long ⇒ A) = new FunctionColumn(name, length, f)

}
