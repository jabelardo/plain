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

  final def take(n: Int, characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, String], Input[Exchange]) =
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

  final def takeBytes(n: Int): Iteratee[Exchange, Array[Byte]] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, Array[Byte]], Input[Exchange]) =
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

  final def peek: Iteratee[Exchange, Byte] = {

    def cont(input: Input[Exchange]): (Iteratee[Exchange, Byte], Input[Exchange]) =
      input match {
        case Elem(more) ⇒ (Done(more.peek), Elem(more))
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont _)
  }

  final def isEof: Iteratee[Exchange, Boolean] = {

    def cont(input: Input[Exchange]): (Iteratee[Exchange, Boolean], Input[Exchange]) =
      input match {
        case Elem(more) if 0 < more.length ⇒ (Done(false), Elem(more))
        case _ ⇒ (Done(true), Eof)
      }

    Cont(cont _)
  }

  final def peek(n: Int, characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, String], Input[Exchange]) =
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

  final def takeWhile(p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, String], Input[Exchange]) =
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

  final def takeUntil(p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] =
    takeWhile(b ⇒ !p(b), characterset, lowercase)

  final def takeUntil(delimiter: Byte, characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, String], Input[Exchange]) =
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

  final def takeUntilCrLf(characterset: Charset, lowercase: Boolean): Iteratee[Exchange, String] = {

    def cont(taken: Exchange)(input: Input[Exchange]): (Iteratee[Exchange, String], Input[Exchange]) =
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

  final def drop(n: Int): Iteratee[Exchange, Unit] = {

    def cont(remaining: Int)(input: Input[Exchange]): (Iteratee[Exchange, Unit], Input[Exchange]) =
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
