package com.ibm.plain

package lib

package aio

/**
 * Input of type E. It will be produced by an Enumerator[E].
 */
trait Input[+E] {

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

  final case class Failure(e: Throwable) extends Input[Nothing]

  final case class Elem[+E](elem: E) extends Input[E]

}

