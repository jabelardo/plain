package com.ibm

package plain

package aio

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration._

import Input.{ Elem, Empty, Eof, Failure }
import Iteratee.{ Cont, Done, Error }

/**
 * The minimum needed Iteratees to fold over a stream of bytes to produce an HttpRequest object.
 */
object Iteratees {

  final def take[A](n: Int, characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], String], Input[ExchangeIo[A]]) =
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

  final def takeBytes[A](n: Int): Iteratee[ExchangeIo[A], Array[Byte]] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], Array[Byte]], Input[ExchangeIo[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          if (in.length < n) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(in.length).consume), Elem(in))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def continue[A](n: Long, continuebuffer: ByteBuffer): Iteratee[ExchangeIo[A], Null] = {

    val written = Promise[Boolean]

    object ContinueWriteHandler

      extends Handler[Integer, ExchangeIo[A]] {

      @inline final def completed(processed: Integer, exchange: ExchangeIo[A]) = written.success(true)

      @inline final def failed(e: Throwable, exchange: ExchangeIo[A]) = written.failure(e)

    }

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], Null], Input[ExchangeIo[A]]) = {
      input match {
        case Elem(more: Exchange[A]) ⇒
          more.socketChannel.write(continuebuffer, more, ContinueWriteHandler)
          Await.ready(written.future, readWriteDuration)
          (Done(null), Elem(more))
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(null), input)
      }
    }

    Cont(cont(null) _)
  }

  final def peek[A]: Iteratee[ExchangeIo[A], Byte] = {

    def cont(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], Byte], Input[ExchangeIo[A]]) =
      input match {
        case Elem(more) ⇒ (Done(more.peek), Elem(more))
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont _)
  }

  final def isEof[A]: Iteratee[ExchangeIo[A], Boolean] = {

    def cont(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], Boolean], Input[ExchangeIo[A]]) =
      input match {
        case Elem(more) if 0 < more.length ⇒ (Done(false), Elem(more))
        case _ ⇒ (Done(true), Eof)
      }

    Cont(cont _)
  }

  final def peek[A](n: Int, characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], String], Input[ExchangeIo[A]]) =
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

  final def takeWhile[A](p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], String], Input[ExchangeIo[A]]) =
      input match {
        case Elem(more) ⇒
          val in = if (null == taken) more else taken ++ more
          val (found, length) = in.span(p)
          if (0 < length) {
            (Done(in.take(found).decode(characterset, lowercase)), Elem(in))
          } else {
            (Cont(cont(in)), Empty)
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(null) _)
  }

  final def takeUntil[A](p: Int ⇒ Boolean, characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] =
    takeWhile(b ⇒ !p(b), characterset, lowercase)

  final def takeUntil[A](delimiter: Byte, characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], String], Input[ExchangeIo[A]]) =
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

  final def takeUntilCrLf[A](characterset: Charset, lowercase: Boolean): Iteratee[ExchangeIo[A], String] = {

    def cont(taken: ExchangeIo[A])(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], String], Input[ExchangeIo[A]]) =
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

  final def drop[A](n: Int): Iteratee[ExchangeIo[A], Unit] = {

    def cont(length: Int)(input: Input[ExchangeIo[A]]): (Iteratee[ExchangeIo[A], Unit], Input[ExchangeIo[A]]) =
      input match {
        case Elem(more) ⇒
          val len = more.length
          if (length > len) {
            (Cont(cont(length - len)), Empty)
          } else {
            (Done(()), Elem(more.drop(length)))
          }
        case Failure(e) ⇒ (Error(e), input)
        case _ ⇒ (Error(EOF), input)
      }

    Cont(cont(n) _)
  }

  private[this] final val EOF = new EOFException("Unexpected EOF.")

}
