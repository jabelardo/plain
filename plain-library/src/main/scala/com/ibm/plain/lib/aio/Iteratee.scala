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

  final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = try {
    this match {
      case Cont(k) ⇒ k(input)
      case it ⇒ (it, input)
    }
  } catch {
    case e: Throwable ⇒ (Error(e), input)
  }

  /**
   * Not really useful, use a match/case instead.
   */
  final def result: A = this(Eof)._1 match {
    case Done(a) ⇒ a
    case Error(e) ⇒ throw e
    case Cont(_) ⇒ throw NotYetDone
  }

  /**
   * All lines in a for-comprehension except the last one.
   */
  @inline final def flatMap[B](f: A ⇒ Iteratee[E, B]): Iteratee[E, B] = this match {
    case Done(a) ⇒ f(a)
    case e @ Error(_) ⇒ e
    case Cont(comp: Compose[E, A]) ⇒ Cont(comp ++ f)
    case Cont(k) ⇒ Cont(Compose(k, f))
  }

  /**
   * The last line in a for-comprehension.
   */
  @inline final def map[B](f: A ⇒ B): Iteratee[E, B] = flatMap(a ⇒ Done(f(a)))

  /**
   * A synonym for flatmap.
   */
  @inline final def >>>[B](f: A ⇒ Iteratee[E, B]) = flatMap(f)

  /**
   * A synonym for map.
   */
  @inline final def >>[B](f: A ⇒ B) = map(f)

}

object Iteratee {

  final case class Done[E, +A](a: A) extends Iteratee[E, A]

  final case class Error[E](e: Throwable) extends Iteratee[E, Nothing]

  final case class Cont[E, A](cont: Input[E] ⇒ (Iteratee[E, A], Input[E])) extends Iteratee[E, A]

  private object Compose {

    @inline def apply[E, A, B](k: Input[E] ⇒ (Iteratee[E, A], Input[E]), f: A ⇒ Iteratee[E, B]) = {
      new Compose[E, B](k, PrimitiveList(f), PrimitiveList.empty)
    }

  }

  /**
   * Most trivial very! low level immutable list implementation with fixed size of 2 (yes 2). Enlarge size on ArrayIndexOutOfBounds exceptions.
   */
  private final class PrimitiveList private (

    tl: Int) {

    def this(a: Any) = { this(1); entries.update(0, a) }

    def this(a: Any, b: Any) = { this(2); entries.update(0, a); entries.update(1, b) }

    def ++(a: Any): PrimitiveList = if (1 == tl) new PrimitiveList(entries(0), a) else new PrimitiveList(a)

    @inline def isEmpty = 0 == tl

    @inline def head: Any = entries(0)

    @inline def tail: PrimitiveList = tl match {
      case 1 ⇒ PrimitiveList.empty
      case 2 ⇒ new PrimitiveList(entries(1))
    }

    private[this] final val entries = new Array[Any](2)

  }

  private object PrimitiveList {

    val empty = new PrimitiveList(0)

    def apply(a: Any) = new PrimitiveList(a)

  }

  /**
   * This class is a performance bottleneck and could use some refinement.
   */
  private final class Compose[E, A] private (

    var k: Input[E] ⇒ (Iteratee[E, _], Input[E]),

    out: PrimitiveList,

    in: PrimitiveList)

    extends (Input[E] ⇒ (Iteratee[E, A], Input[E])) {

    @inline final def ++[B](f: _ ⇒ Iteratee[E, B]) = new Compose[E, B](k, out, in ++ f)

    /**
     * A plain application spends most of its time in this method.
     */
    final def apply(input: Input[E]): (Iteratee[E, A], Input[E]) = {

      @inline @tailrec def run(
        result: (Iteratee[E, _], Input[E]),
        out: PrimitiveList,
        in: PrimitiveList): (Iteratee[E, _], Input[E]) = {
        if (out.isEmpty) {
          if (in.isEmpty) result else run(result, in, PrimitiveList.empty)
        } else {
          result match {
            case (Done(value), remaining) ⇒
              out.head.asInstanceOf[Any ⇒ Iteratee[E, _]](value) match {
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

  private final lazy val NotYetDone = new IllegalStateException("Not yet done.")

}

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  import Io._
  import Iteratee._

  def take(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(n).decode), Elem(in.drop(n)))
        }
    }
    Cont(cont(Io.empty))
  }

  def peek(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Failure(e) ⇒ (Error(e), input)
      case Eof ⇒
        (Done(taken.decode), Eof)
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.peek(n).decode), Elem(in))
        }
    }
    Cont(cont(Io.empty))
  }

  def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = taken ++ more
        val (found, remaining) = in.span(p)
        if (0 < remaining) {
          (Done(in.take(found).decode), Elem(in))
        } else {
          (Cont(cont(in)), Empty)
        }
    }
    Cont(cont(Io.empty))
  }

  def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = takeWhile(b ⇒ !p(b))(cset)

  def takeUntil(delimiter: Byte)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val in = taken ++ more
        val pos = in.indexOf(delimiter)
        if (0 > pos) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(pos).decode), Elem(in.drop(1)))
        }
    }
    Cont(cont(Io.empty))
  }

  def drop(n: Int): Iteratee[Io, Unit] = {
    def cont(remaining: Int)(input: Input[Io]): (Iteratee[Io, Unit], Input[Io]) = input match {
      case Eof ⇒ (Error(EOF), input)
      case Failure(e) ⇒ (Error(e), input)
      case Elem(more) ⇒
        val len = more.length
        if (remaining > len) {
          (Cont(cont(remaining - len)), Empty)
        } else {
          (Done(()), Elem(more.drop(remaining)))
        }
    }
    Cont(cont(n))
  }

  private[this] final lazy val EOF = new EOFException("Unexpected EOF")

}

