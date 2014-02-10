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

  final def take(n: Int)(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(n).decode), Elem(in))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  private[this] object ContinueWriteHandler

    extends java.nio.channels.CompletionHandler[Integer, Io] {

    @inline def completed(processed: Integer, io: Io) = io.writebuffer.clear

    @inline def failed(e: Throwable, io: Io) = io.writebuffer.clear

    final val response = "HTTP/1.1 100 Continue\r\n\r\n".getBytes

  }

  final def takeBytes(n: Int): Iteratee[Io, Array[Byte]] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, Array[Byte]], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (0 == in.length) {
          in.writebuffer.put(ContinueWriteHandler.response)
          in.writebuffer.flip
          in.channel.write(in.writebuffer, in, ContinueWriteHandler)
          (Cont(cont(in)), Empty)
        } else {
          if (in.length < n) {
            (Cont(cont(in)), Empty)
          } else {
            (Done(in.take(n).consume), Elem(in))
          }
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def peek: Iteratee[Io, Byte] = {
    def cont(input: Input[Io]): (Iteratee[Io, Byte], Input[Io]) = input match {
      case Elem(more) ⇒ (Done(more.peek), Elem(more))
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont _)
  }

  final def isEof: Iteratee[Io, Boolean] = {
    def cont(input: Input[Io]): (Iteratee[Io, Boolean], Input[Io]) = input match {
      case Elem(more) if 0 < more.readbuffer.remaining ⇒ (Done(false), Elem(more))
      case _ ⇒ (Done(true), Eof)
    }
    Cont(cont _)
  }

  final def peek(n: Int)(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        if (in.length < n) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.peek(n).decode), Elem(in))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Done(taken.decode), Eof)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeWhile(p: Int ⇒ Boolean)(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val (found, remaining) = in.span(p)
        if (0 < remaining) {
          (Done(in.take(found).decode), Elem(in))
        } else {
          (Cont(cont(in)), Empty)
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeUntil(p: Int ⇒ Boolean)(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = takeWhile(b ⇒ !p(b))(cset, lowercase)

  final def takeUntil(delimiter: Byte)(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val pos = in.indexOf(delimiter)
        if (0 > pos) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(pos).decode), Elem(in.drop(1)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(Io.empty) _)
  }

  final def takeUntilCrLf(implicit cset: Charset, lowercase: Boolean): Iteratee[Io, String] = {
    def cont(taken: Io)(input: Input[Io]): (Iteratee[Io, String], Input[Io]) = input match {
      case Elem(more) ⇒
        val in = taken ++ more
        val pos = in.indexOf(`\r`)
        if (0 > pos) {
          (Cont(cont(in)), Empty)
        } else {
          (Done(in.take(pos).decode), Elem(in.drop(2)))
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
          (Cont(cont(remaining - len)), Empty)
        } else {
          (Done(()), Elem(more.drop(remaining)))
        }
      case Failure(e) ⇒ (Error(e), input)
      case _ ⇒ (Error(EOF), input)
    }
    Cont(cont(n) _)
  }

  final val EOF = new EOFException("Unexpected EOF")

  private[this] final val `\r` = '\r'.toByte

}

