package com.ibm

package plain

package aio

import java.io.EOFException
import java.nio.charset.Charset

import scala.annotation.tailrec

import Input.{ Elem, Empty, Eof, Failure }
import Iteratee.{ Cont, Done, Error }

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  final def take(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in) _), Empty)
        } else {
          (Done(in.take(n).decode), Elem(in.drop(n)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeBytes(n: Int) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, Array[Byte]], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in) _), Empty)
        } else {
          (Done({ in.take(n); in.readAllBytes }), Elem(in.drop(n)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def peek(n: Int)(implicit cset: Charset) = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in) _), Empty)
        } else {
          (Done(in.peek(n).decode), Elem(in))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Done(taken.decode), Eof)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val (found, remaining) = in.span(p)
        if (0 < remaining) {
          (Done(in.take(found).decode), Elem(in))
        } else {
          (Cont(cont(in) _), Empty)
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset): Iteratee[Io, String] = takeWhile(b ⇒ !p(b))(cset)

  final def takeUntil(delimiter: Byte)(implicit cset: Charset): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val pos = in.indexOf(delimiter)
        if (0 > pos) {
          (Cont(cont(in) _), Empty)
        } else {
          (Done(in.take(pos).decode), Elem(in.drop(1)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def drop(n: Int): Iteratee[Io, Unit] = {
    def cont(remaining: Int)(input: Input[Io]): (Iteratee[Io, Unit], Input[Io]) = input match {
      case Elem(more) ⇒
        val len = more.length
        if (remaining > len) {
          (Cont(cont(remaining - len) _), Empty)
        } else {
          (Done(()), Elem(more.drop(remaining)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(n) _)
  }

  final val EOF = new EOFException("Unexpected EOF")

}

