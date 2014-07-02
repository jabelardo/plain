package com.ibm

package plain

package aio

import scala.annotation.tailrec
import scala.util.control.ControlThrowable

import Input.Eof

/**
 * An Iteratee consumes elements of type Input[E] and produces a result of type A.
 */
sealed trait Iteratee[E, +A]

  extends Any {

  import Iteratee._

  def apply(input: Input[E]): (Iteratee[E, A], Input[E])

  @inline final def result: A = this(Eof) match {
    case (Done(a), _) ⇒ a
    case (Error(e), _) ⇒ throw e
    case _ ⇒ throw IllegalState
  }

  def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B]

  def map[B](f: A ⇒ B): Iteratee[E, B]

}

object Iteratee {

  final class Done[E, A] private (val a: A)

    extends Iteratee[E, A] {

    @inline final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = (this, input)

    @inline final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = f(a)

    @inline final def map[B](f: A ⇒ B): Iteratee[E, B] = Done(f(a))

  }

  object Done {

    @inline final def apply[E, A](a: A) = new Done[E, A](a)

    @inline final def unapply[E, A](done: Done[E, A]): Option[A] = Some(done.a)

  }

  final class Error[E] private (val e: Throwable)

    extends AnyVal

    with Iteratee[E, Nothing] {

    @inline final def apply(input: Input[E]): (Iteratee[E, Nothing], Input[E]) = (this, input)

    @inline final def flatMap[B](f: Nothing ⇒ Iteratee[E, B]): Iteratee[E, B] = this

    @inline final def map[B](f: Nothing ⇒ B): Iteratee[E, B] = this

  }

  object Error {

    @inline final def apply[E](e: Throwable) = new Error[E](e)

    @inline final def unapply[E](error: Error[E]): Option[Throwable] = Some(error.e)

  }

  final class Cont[E, A] private (val k: Input[E] ⇒ (Iteratee[E, A], Input[E]))

    extends AnyVal

    with Iteratee[E, A] {

    @inline final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = k(input)

    final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = k match {
      case comp: Compose[E, B] ⇒ Cont(comp.clone(f.asInstanceOf[Any ⇒ Iteratee[E, _]]))
      case k ⇒ Cont(new Compose(k, f.asInstanceOf[Any ⇒ Iteratee[E, _]] :: Nil, Nil))
    }

    final def map[B](f: A ⇒ B): Iteratee[E, B] = flatMap(a ⇒ Done[E, B](f(a)))

  }

  object Cont {

    @inline final def apply[E, A](k: Input[E] ⇒ (Iteratee[E, A], Input[E])) = new Cont[E, A](k)

    @inline final def unapply[E, A](cont: Cont[E, A]): Option[Input[E] ⇒ (Iteratee[E, A], Input[E])] = Some(cont.k)

  }

  private final class Compose[E, A](

    private[this] final val k: R[E, _],

    private[this] final val out: List[Any ⇒ Iteratee[E, _]],

    private[this] final val in: List[Any ⇒ Iteratee[E, _]])

    extends R[E, A] {

    @inline final def clone(f: Any ⇒ Iteratee[E, _]) = { new Compose(k, out, f :: in) }

    final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = {
      @inline @tailrec def run(
        result: (Iteratee[E, _], Input[E]),
        out: List[Any ⇒ Iteratee[E, _]],
        in: List[Any ⇒ Iteratee[E, _]]): (Iteratee[E, _], Input[E]) = {
        if (out.isEmpty) {
          if (in.isEmpty) {
            result
          } else {
            run(result, if (1 < in.length) in.reverse else in, Nil)
          }
        } else {
          result match {
            case (Done(value), remaining) ⇒ out.head(value) match {
              case Cont(k) ⇒ run(k(remaining), out.tail, in)
              case e ⇒ run((e, remaining), out.tail, in)
            }
            case (Cont(k), remaining) ⇒ (Cont(new Compose(k, out, in)), remaining)
            case _ ⇒ result
          }
        }
      }

      run(k(input), out, in).asInstanceOf[(Iteratee[E, A], Input[E])]
    }

  }

  private final type R[E, A] = Input[E] ⇒ (Iteratee[E, A], Input[E])

  private final val IllegalState = new IllegalStateException with ControlThrowable

}
