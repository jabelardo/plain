package com.ibm.plain

package lib

package aio

import java.io.EOFException
import java.nio.charset.Charset

import scala.annotation.tailrec

import Input.{ Elem, Empty, Eof, Failure }

/**
 * An iteratee consumes a stream of elements of type Input[E] and produces a result of type A.
 */
sealed abstract class Iteratee[E, +A] {

  import Iteratee._

  final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = this match {
    case Cont(f) ⇒ f(input)
    case it ⇒ (it, input)
  }

  final def result: A = this(Eof)._1 match {
    case Done(a) ⇒ a
    case Error(e) ⇒ throw e
    case Cont(_) ⇒ throw notyetdone
  }

  final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = this match {
    case Done(a) ⇒ f(a)
    case e @ Error(_) ⇒ e
    case Cont(k: Compose[E, A]) ⇒ Cont(k ++ f)
    case Cont(k) ⇒ Cont(Compose(k, f))
  }

  final def map[B](f: A ⇒ B): Iteratee[E, B] = flatMap(a ⇒ Done(f(a)))

  final def >>>[B](f: A ⇒ Iteratee[E, B]) = flatMap(f)

  final def >>[B](f: A ⇒ B) = map(f)

}

object Iteratee {

  private object Compose {

    def apply[E, A](k: Input[E] ⇒ (Iteratee[E, A], Input[E])) = new Compose[E, A](k, Nil, Nil)

    def apply[E, A, B](k: Input[E] ⇒ (Iteratee[E, A], Input[E]), f: A ⇒ Iteratee[E, B]) =
      new Compose[E, B](k, (f.asInstanceOf[Any ⇒ Iteratee[E, Any]]) :: Nil, Nil)

  }

  private final case class Compose[E, +A] private (
    k: Input[E] ⇒ (Iteratee[E, Any], Input[E]),
    out: List[Any ⇒ Iteratee[E, Any]],
    in: List[Any ⇒ Iteratee[E, Any]])

    extends (Input[E] ⇒ (Iteratee[E, A], Input[E])) {

    def ++[B](f: A ⇒ Iteratee[E, B]) = new Compose[E, B](k, out, f.asInstanceOf[Any ⇒ Iteratee[E, Any]] :: in)

    def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = {

      @inline @tailrec def run(
        result: (Iteratee[E, Any], Input[E]),
        out: List[Any ⇒ Iteratee[E, Any]],
        in: List[Any ⇒ Iteratee[E, Any]]): (Iteratee[E, Any], Input[E]) = {
        if (out.isEmpty) {
          if (in.isEmpty) result else run(result, in.reverse, Nil)
        } else
          result match {
            case (Done(value), remaining) ⇒
              out.head(value) match {
                case Cont(k) ⇒ run(k(remaining), out.tail, in)
                case iter ⇒ run((iter, remaining), out.tail, in)
              }
            case (Cont(k), remaining) ⇒ (Cont(new Compose(k, out, in)), remaining)
            case _ ⇒ result
          }
      }

      run(k(input), out, in).asInstanceOf[(Iteratee[E, A], Input[E])]
    }

  }

  private final val notyetdone = new IllegalStateException("Not yet done.")

}

final case class Done[E, +A](a: A) extends Iteratee[E, A]

final case class Error[E](e: Throwable) extends Iteratee[E, Nothing]

final case class Cont[E, +A](cont: Input[E] ⇒ (Iteratee[E, A], Input[E])) extends Iteratee[E, A]

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce a HttpRequest object.
 */
object Iteratees {

  def take(n: Int)(implicit cset: Charset) = iter(n)(cset)(in ⇒ in.take(n))

  def peek(n: Int)(implicit cset: Charset) = iter(n)(cset)(in ⇒ in.peek(n))

  def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[ByteBufferInput, String] = {
    def cont(taken: ByteBufferInput)(input: Input[ByteBufferInput]): (Iteratee[ByteBufferInput, String], Input[ByteBufferInput]) = input match {
      case Eof | Empty ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val found = more.takeWhile(p)
        if (0 < more.remaining) {
          (Done((taken ++ found).decode(cset)), Elem(more))
        } else {
          (Cont(cont(taken ++ found)), Empty)
        }
    }
    Cont(cont(ByteBufferInput.empty))
  }

  def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[ByteBufferInput, String] = takeWhile(b ⇒ !p(b))

  def takeUntil(delimiter: Byte)(implicit cset: Charset): Iteratee[ByteBufferInput, String] = {
    def cont(taken: ByteBufferInput)(input: Input[ByteBufferInput]): (Iteratee[ByteBufferInput, String], Input[ByteBufferInput]) = input match {
      case Eof | Empty ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val found = more.takeUntil(delimiter)
        if (0 < more.remaining) {
          (Done((taken ++ found).decode(cset)), Elem(more.drop(1)))
        } else {
          (Cont(cont(taken ++ found)), Empty)
        }
    }
    Cont(cont(ByteBufferInput.empty))
  }

  def takeUntil(delimiter: Array[Byte])(implicit cset: Charset): Iteratee[ByteBufferInput, String] = {
    def cont(taken: ByteBufferInput)(input: Input[ByteBufferInput]): (Iteratee[ByteBufferInput, String], Input[ByteBufferInput]) = input match {
      case Eof | Empty ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val found = more.takeUntil(delimiter)
        if (0 < more.remaining) {
          (Done((taken ++ found).decode(cset)), Elem(more.drop(delimiter.length)))
        } else {
          (Cont(cont(taken ++ found)), Empty)
        }
    }
    Cont(cont(ByteBufferInput.empty))
  }

  def drop(n: Int): Iteratee[ByteBufferInput, Unit] = {
    def cont(left: Int)(input: Input[ByteBufferInput]): (Iteratee[ByteBufferInput, Unit], Input[ByteBufferInput]) = input match {
      case Eof | Empty ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        if (left <= more.length) {
          (Done(()), Elem(more.drop(left)))
        } else {
          (Cont(cont(left - more.length)), Empty)
        }
    }
    Cont(cont(n))
  }

  private[this] def iter(n: Int)(cset: Charset)(f: ByteBufferInput ⇒ ByteBufferInput): Iteratee[ByteBufferInput, String] = {
    def cont(taken: ByteBufferInput)(input: Input[ByteBufferInput]): (Iteratee[ByteBufferInput, String], Input[ByteBufferInput]) = input match {
      case Eof | Empty ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length >= n) {
          (Done(f(in).decode(cset)), Elem(in))
        } else {
          (Cont(cont(in)), Empty)
        }
    }
    Cont(cont(ByteBufferInput.empty))
  }

  private[this] val EOF = new EOFException

}

