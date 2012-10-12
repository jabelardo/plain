package com.ibm.asynctest

package iteratee

/**
 * A producer of input of type E which it pushes into an Iteratee[E, A].
 */
trait Enumerator[E] {

  //  def apply[A](i: Iteratee[E, A]): Enumerator.Io @suspendable

}

/**
 * Constructing different types of Enumerator[E].
 */
object Enumerator {

  //  def once[E](input: Input[E]): Enumerator[E] = new Enumerator[E] {
  //
  //    def apply[A](it: Iteratee[E, A]) = it(input)
  //
  //  }

  //  def repeat[E](input: ⇒ Input[E]): Enumerator[E] = new Enumerator[E] {
  //
  //    def apply[A](it: Iteratee[E, A]) = it(input) match {
  //      case (c @ Cont(_), _) ⇒ apply(c)
  //      case e ⇒ e
  //    }
  //
  //  }

}

/**
 * Here we become 'monadic', fasten your seat belts.
 */
case class Inp[E, A](f: E ⇒ A)

trait Mon[R] {

  def unit[A](a: A): Nothing

}

/**
 * what the ...?
 */
class BMon[R] extends Mon[({ type λ[α] = Inp[α, R] })#λ[_]] {

  override def unit[A](a: A) = throw new Exception

}

