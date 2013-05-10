package com.ibm

package plain

package aio

import java.io.EOFException
import java.nio.charset.Charset

import scala.annotation.tailrec
import scala.math.max

import Input.{ Elem, Empty, Eof, Failure }

/**
 * An iteratee consumes a stream of elements of type Input[E] and produces a result of type A.
 */
sealed trait Iteratee[E, +A]

  extends Any {

  import Iteratee._

  final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = try {
    this match {
      case Cont(k) ⇒ k(input)
      case it ⇒ (it, input)
    }
  } catch {
    case e: Throwable ⇒ (Error(e), input)
  }

  /**
   * Used later at api level, e.g. HttpProcessor, and is very useful, therefore.
   */
  final def result: A = this(Eof)._1 match {
    case Done(a) ⇒ a
    case Error(e) ⇒ throw e
    case Cont(_) ⇒ throw NotYetDone
  }

  /**
   * All lines in a for-comprehension except the last one.
   */
  final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = this match {
    case Cont(comp: Compose[E, B]) ⇒ Cont(comp ++ f.asInstanceOf[Any ⇒ Iteratee[E, B]])
    case Cont(k) ⇒ Cont(new Compose(k, f.asInstanceOf[Any ⇒ Iteratee[E, _]]))
    case Done(a) ⇒ f(a)
    case e @ Error(_) ⇒ e
  }

  /**
   * The last line in a for-comprehension.
   */
  final def map[B](f: A ⇒ B): Iteratee[E, B] = flatMap(a ⇒ new Done(f(a)))

  /**
   * A synonym for flatmap.
   */
  final def >>>[B](f: A ⇒ Iteratee[E, B]) = flatMap(f)

  /**
   * A synonym for map.
   */
  final def >>[B](f: A ⇒ B) = map(f)

}

object Iteratee {

  final class Done[E, A](val a: A)

    extends Iteratee[E, A]

  object Done {

    @inline final def apply[E, A](a: A) = new Done[E, A](a)

    @inline final def unapply[E, A](done: Done[E, A]): Option[A] = Some(done.a)

  }

  final case class Cont[E, A](cont: Input[E] ⇒ (Iteratee[E, A], Input[E]))

    extends AnyVal

    with Iteratee[E, A]

  final case class Error[E](e: Throwable)

    extends AnyVal

    with Iteratee[E, Nothing]

  /**
   * private helpers
   */
  private final type R[E, A] = Input[E] ⇒ (Iteratee[E, A], Input[E])

  /**
   * This class is a performance bottleneck and could use some refinement.
   */
  private final class Compose[E, A] private (

    private[this] final val k: R[E, _],

    private[this] final val out: List[Any ⇒ Iteratee[E, _]],

    private[this] final val in: List[Any ⇒ Iteratee[E, _]])

    extends R[E, A] {

    def this(k: R[E, _], f: Any ⇒ Iteratee[E, _]) = this(k, f :: Nil, Nil)

    @inline final def ++[B](f: Any ⇒ Iteratee[E, B]): R[E, B] = {
      new Compose[E, B](k, out, f :: in)
    }

    /**
     * A plain application spends most of its time in this method.
     */
    final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = {

      @inline @tailrec def run(
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

  private final val NotYetDone = new IllegalStateException("Not yet done.")

}

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  import Io._
  import Iteratee._

  final def take(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(n).decode), Elem(in.drop(n)))
        }
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(Io.empty))
  }

  final def takeBytes(n: Int) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, Array[Byte]], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done({ in.take(n); in.readAllBytes }), Elem(in.drop(n)))
        }
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(Io.empty))
  }

  final def peek(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.peek(n).decode), Elem(in))
        }
      case Eof ⇒
        (Done(taken.decode), Eof)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(Io.empty))
  }

  final def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val (found, remaining) = in.span(p)
        if (0 < remaining) {
          (Done(in.take(found).decode), Elem(in))
        } else {
          (Cont(cont(in)), Empty)
        }
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(Io.empty))
  }

  final def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = takeWhile(b ⇒ !p(b))(cset)

  final def takeUntil(delimiter: Byte)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val pos = in.indexOf(delimiter)
        if (0 > pos) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(pos).decode), Elem(in.drop(1)))
        }
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(Io.empty))
  }

  final def drop(n: Int): Iteratee[Io, Unit] = {
    def cont(remaining: Int)(input: Input[Io]): (Iteratee[Io, Unit], Input[Io]) = input match {
      case Elem(more) ⇒
        val len = more.length
        if (remaining > len) {
          (Cont(cont(remaining - len)), Empty)
        } else {
          (Done(()), Elem(more.drop(remaining)))
        }
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
    }
    Cont(cont(n))
  }

  final val EOF = new EOFException("Unexpected EOF")

}

