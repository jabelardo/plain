package com.ibm.plain

package lib

package aio

import java.io.EOFException
import java.nio.charset.Charset

import scala.annotation.tailrec
import scala.math.max

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
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  import Io._

  def take(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = more ++ (taken.bytestring ++ more.bytestring)
        if (in.bytestring.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.bytestring.take(n).decodeString(cset.toString)), Elem(in ++ in.bytestring.drop(n)))
        }
    }
    Cont(cont(Io.empty))
  }

  def peek(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Failure(e) ⇒ println("error " + e); (Error(e), input)
      case Eof ⇒
        (Done(taken.bytestring.decodeString(cset.toString)), Eof)
      case Elem(more) ⇒
        val in = more ++ (taken.bytestring ++ more.bytestring)
        if (in.bytestring.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.bytestring.take(n).decodeString(cset.toString)), Elem(in))
        }
    }
    Cont(cont(Io.empty))
  }

  def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val pp: Byte ⇒ Boolean = b ⇒ p(b)
        val (found, remaining) = more.bytestring.span(pp)
        if (remaining.isEmpty) {
          (Cont(cont(more ++ (taken.bytestring ++ found))), Empty)
        } else {
          (Done((more ++ (taken.bytestring ++ found)).bytestring.decodeString(cset.toString)), Elem(more ++ remaining))
        }
    }
    Cont(cont(Io.empty))
  }

  def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = takeWhile(b ⇒ !p(b))(cset)

  def takeUntil(delimiter: Array[Byte])(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = more ++ (taken.bytestring ++ more.bytestring)
        val pos = in.bytestring.indexOfSlice(delimiter, max(taken.bytestring.length - delimiter.length, 0))
        if (0 <= pos) {
          (Done(in.bytestring.take(pos).decodeString(cset.toString)), Elem(in ++ in.bytestring.drop(pos + delimiter.length)))
        } else {
          (Cont(cont(in)), Empty)
        }
    }
    Cont(cont(Io.empty))
  }

  def drop(n: Int): Iteratee[Io, Unit] = {
    def cont(remaining: Int)(input: Input[Io]): (Iteratee[Io, Unit], Input[Io]) = input match {
      case Eof ⇒ throw EOF
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        if (remaining > more.bytestring.length) {
          (Cont(cont(remaining - more.bytestring.length)), Empty)
        } else {
          (Done(()), Elem(more ++ more.bytestring.drop(remaining)))
        }
    }
    Cont(cont(n))
  }

  private[this] lazy val EOF = new EOFException

}

