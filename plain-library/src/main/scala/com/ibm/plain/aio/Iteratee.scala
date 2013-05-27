package com.ibm

package plain

package aio

import java.io.EOFException
import java.nio.charset.Charset

import scala.annotation.tailrec
import scala.math.max

import Input.{ Elem, Empty, Eof, Failure }

/**
 * An Iteratee consumes elements of type Input[E] and produces a result of type A.
 */
sealed trait Iteratee[E, +A]

  extends Any {

  import Iteratee._

  def apply(input: Input[E]): (Iteratee[E, A], Input[E])

  final def result: A = this(Eof) match {
    case (Done(a), _) ⇒ a
    case (Error(e), _) ⇒ throw e
    case _ ⇒ throw new IllegalStateException
  }

  def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B]

  def map[B](f: A ⇒ B): Iteratee[E, B]

}

object Iteratee {

  final case class Done[E, A](a: A)

    extends AnyVal with Iteratee[E, A] {

    final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = (this, input)

    final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = f(a)

    final def map[B](f: A ⇒ B): Iteratee[E, B] = Done(f(a))

  }

  final case class Cont[E, A](k: Input[E] ⇒ (Iteratee[E, A], Input[E]))

    extends AnyVal

    with Iteratee[E, A] {

    final override def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = try {
      k(input)
    } catch {
      case e: Throwable ⇒ (Error(e), input)
    }

    final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = {
      k match {
        case comp: Compose[E, B] ⇒ Cont(comp ++ f.asInstanceOf[Any ⇒ Iteratee[E, B]])
        case _ ⇒ Cont(new Compose(k, f.asInstanceOf[Any ⇒ Iteratee[E, _]] :: Nil, Nil))
      }
    }

    final def map[B](f: A ⇒ B): Iteratee[E, B] = flatMap(a ⇒ g(a, f))

    @inline final def g[B](a: A, f: A ⇒ B): Iteratee[E, B] = Done(f(a))

  }

  final case class Error[E](e: Throwable)

    extends AnyVal

    with Iteratee[E, Nothing] {

    final def apply(input: Input[E]): (Iteratee[E, Nothing], Input[E]) = (this, input)

    final def flatMap[B](f: Nothing ⇒ Iteratee[E, B]): Iteratee[E, B] = this

    final def map[B](f: Nothing ⇒ B): Iteratee[E, B] = this

  }

  /**
   * private helpers
   */
  private final type R[E, A] = Input[E] ⇒ (Iteratee[E, A], Input[E])

  /**
   * This class is a performance bottleneck and could use some refinement.
   */
  private[aio] final class Compose[E, A](

    private[this] final val k: R[E, _],

    private[this] final val out: List[Any ⇒ Iteratee[E, _]],

    private[this] final val in: List[Any ⇒ Iteratee[E, _]])

    extends R[E, A] {

    final def ++[B](f: Any ⇒ Iteratee[E, B]): R[E, B] = new Compose[E, B](k, out, f :: in)

    final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = {

      @tailrec def run(
        result: (Iteratee[E, _], Input[E]),
        out: List[Any ⇒ Iteratee[E, _]],
        in: List[Any ⇒ Iteratee[E, _]]): (Iteratee[E, _], Input[E]) = {
        if (out.isEmpty) {
          if (in.isEmpty) result else run(result, in match { case Nil ⇒ Nil case _ :: Nil ⇒ in case _ ⇒ in.reverse }, Nil)
        } else {
          result match {
            case (Done(value), remaining) ⇒
              out.head(value) match {
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

}
