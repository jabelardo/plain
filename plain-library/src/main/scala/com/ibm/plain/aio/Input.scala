package com.ibm

package plain

package aio

/**
 * Input of type E. It will be produced by an Enumerator[E].
 */
trait Input[+E]

  extends Any {

  import Input._

  def map[B](f: E ⇒ B): Input[B] = this match {
    case Eof ⇒ Eof
    case Empty ⇒ Empty
    case e @ Failure(_) ⇒ e
    case Elem(elem) ⇒ Elem(f(elem))
  }

}

object Input {

  final case object Eof extends Input[Nothing]

  final case object Empty extends Input[Nothing]

  final case class Failure(e: Throwable) extends AnyVal with Input[Nothing]

  final case class Elem[+E](elem: E) extends AnyVal with Input[E]

}

