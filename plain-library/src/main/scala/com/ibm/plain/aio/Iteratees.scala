package com.ibm

package plain

package aio

import java.io.EOFException
import java.nio.charset.Charset

import Input.{ Elem, Empty, Eof, Failure }
import Iteratee.{ Cont, Done, Error }

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  final def take[A](n: Int, characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], String], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          if (in.length < n) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(n).decode(characterset, lowercase)), Elem(in))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def takeBytes[A](n: Int): Iteratee[Exchange[A], Array[Byte]] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], Array[Byte]], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          if (in.length < n) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(n).consume), Elem(in))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def peek[A]: Iteratee[Exchange[A], Byte] = {

    def cont(input: Input[Exchange[A]]): (Iteratee[Exchange[A], Byte], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒ (Done(more.peek), Elem(more))
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont _)
  }

  final def isEof[A]: Iteratee[Exchange[A], Boolean] = {

    def cont(input: Input[Exchange[A]]): (Iteratee[Exchange[A], Boolean], Input[Exchange[A]]) =
      input match {
        case Elem(more) if 0 < more.length ⇒ (Done(false), Elem(more))
        case _ ⇒ (Done(true), Eof)
      }

    Cont(cont _)
  }

  final def peek[A](n: Int, characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], String], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          if (in.length < n) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.peek(n).decode(characterset, lowercase)), Elem(in))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Done(taken.decode(characterset, lowercase)), Eof)
      }

    Cont(cont(null) _)
  }

  final def takeWhile[A](p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], String], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          val (found, remaining) = in.span(p)
          if (0 < remaining) {
            (Done(in.take(found).decode(characterset, lowercase)), Elem(in))
          } else {
            (Cont(cont(in)), Empty)
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def takeUntil[A](p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] =
    takeWhile(b ⇒ !p(b), characterset, lowercase)

  final def takeUntil[A](delimiter: Byte, characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], String], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          val pos = in.indexOf(delimiter)
          if (0 > pos) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(pos).decode(characterset, lowercase)), Elem(in.drop(1)))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def takeUntilCrLf[A](characterset: Charset, lowercase: Boolean): Iteratee[Exchange[A], String] = {

    def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], String], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          val pos = in.indexOf('\r'.toByte)
          if (0 > pos) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(pos).decode(characterset, lowercase)), Elem(in.drop(2)))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def drop[A](n: Int): Iteratee[Exchange[A], Unit] = {

    def cont(remaining: Int)(input: Input[Exchange[A]]): (Iteratee[Exchange[A], Unit], Input[Exchange[A]]) =
      input match {
        case Elem(more) ⇒
          val len = more.length
          if (remaining > len) {
            (Cont(cont(remaining - len)), Empty)
          } else {
            (Done(()), Elem(more.drop(remaining)))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(n) _)
  }

  private[this] final val EOF = new EOFException("Unexpected EOF.")

}
