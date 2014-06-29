package com.ibm

package plain

package hybriddb

package schema

package column

/**
 *
 */
@SerialVersionUID(1L) final class FunctionColumn[A](

  val length: Long,

  private[this] final val f: Long ⇒ A)

    extends Column[A] {

  final def get(index: Long): A = f(index)

}

