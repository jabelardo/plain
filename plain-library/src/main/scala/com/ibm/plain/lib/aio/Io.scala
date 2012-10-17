package com.ibm.plain

package lib

package aio

import language.implicitConversions

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset

import scala.util.continuations.{ reset, shift, suspendable }
import scala.annotation.tailrec
import scala.math.min

import text.{ ASCII, UTF8 }

import logging.HasLogger

import aio.Input.{ Elem, Eof, Empty }

/**
 * Helper for Io with all low-level ByteBuffer methods.
 */
abstract sealed class IoHelper[E <: Io] {

  private[this] val self: E = this.asInstanceOf[E]

  import self._

  final def decode(implicit cset: Charset): String = resetBuffer(new String(readBytes, cset))

  final def length: Int = buffer.remaining

  final def take(n: Int): Io = {
    markLimit
    buffer.limit(min(buffer.limit, buffer.position + n))
    self
  }

  final def peek(n: Int): Io = {
    markPosition
    take(n)
  }

  final def drop(n: Int): Io = {
    buffer.position(min(buffer.limit, buffer.position + n))
    self
  }

  final def indexOf(b: Byte): Int = {
    val p = buffer.position
    val l = buffer.limit
    var i = p
    while (i < l && b != buffer.get(i)) i += 1
    if (i == l) -1 else i - p
  }

  final def span(p: Int ⇒ Boolean): (Int, Int) = {
    val pos = buffer.position
    val l = buffer.limit
    var i = pos
    while (i < l && p(buffer.get(i))) i += 1
    (i - pos, l - i)
  }

  @inline protected final def readBytes: Array[Byte] = Array.fill(buffer.remaining)(buffer.get)

  @inline private[this] final def markLimit = limitmark = buffer.limit

  @inline private[this] final def markPosition = positionmark = buffer.position

  @inline private[this] final def resetBuffer[A](a: A): A = {
    require(-1 < limitmark)
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
  }

  /**
   * debug helper
   */
  //  final def render[A](a: A): A = { println(a + " [" + asString + "]" + buffer + " " + limitmark + " " + positionmark); a }

  /**
   * debug helper
   */
  //  final def asString = {
  //    val a = new Array[Byte](buffer.remaining)
  //    val p = buffer.position
  //    for (i ← 0 until buffer.remaining) a.update(i, buffer.get(p + i))
  //    new String(a)
  //  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

}

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final case class Io(

  server: ServerChannel,

  channel: Channel,

  buffer: ByteBuffer,

  iteratee: Iteratee[Io, _],

  k: Io.IoHandler,

  readwritten: Int,

  expected: Long)

  extends IoHelper[Io] {

  val self = this

  import Io._

  @inline def ++(server: ServerChannel) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(channel: Channel) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(buffer: ByteBuffer) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(iteratee: Iteratee[Io, _]) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(k: IoHandler) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(readwritten: Int) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(expected: Long) = Io(server, channel, buffer, iteratee, k, readwritten, expected)

  /**
   * The trick method of the entire algorithm, it should be called only when the buffer is too small and on start with Io.empty.
   */
  final def ++(that: Io): Io = if (0 == this.length) {
    that
  } else if (0 == that.length) {
    this
  } else {
    val len = this.length + that.length
    val b = bestFitByteBuffer(len)
    b.put(this.readBytes)
    releaseByteBuffer(this.buffer)
    b.put(that.buffer)
    releaseByteBuffer(that.buffer)
    b.flip
    that ++ b
  }

}

/**
 * The Io object contains all the complex continuations stuff.
 */
object Io

  extends HasLogger {

  /**
   * Helpers
   */
  final val empty = Io(null, null, ByteBuffer.allocateDirect(0), null, null, -1, -1)

  type IoHandler = Io ⇒ Unit

  /**
   * Aio handling.
   */
  private[this] val accepthandler = new Handler[Channel, Io] {

    def completed(c: Channel, io: Io) = try {
      import io._
      server.accept(io, this)
      k(io ++ c ++ defaultByteBuffer)
    } catch {
      case e: Throwable ⇒ e.printStackTrace
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      if (server.isOpen) {
        server.accept(io, this)
        warning("accept failed : " + e)
      }
    }

  }

  private[this] val iohandler = new Handler[Integer, Io] {

    def completed(n: Integer, io: Io) = try {
      import io._
      if (-1 < n) buffer.flip else channel.close
      k(io ++ n)
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      channel.close
      k(io ++ Error[Io](e))
    }

  }

  final def accept(server: ServerChannel): Io @suspendable =
    shift { k: IoHandler ⇒ server.accept(Io.empty ++ server ++ k, accepthandler) }

  final def read(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ buffer.clear; channel.read(buffer, io ++ k, iohandler) }
  }

  final def handle(io: Io): Unit @suspendable = {
    (read(io) match {
      case io if -1 < io.readwritten ⇒ io.iteratee(Elem(io))
      case io ⇒ io.iteratee(Eof)
    }) match {
      case (cont @ Cont(_), Empty) ⇒
        handle(io ++ cont ++ defaultByteBuffer)
      case (e @ Done(a), el @ Elem(io)) ⇒
        releaseByteBuffer(io.buffer)
        respond(io ++ ByteBuffer.wrap(response))
        handle(io ++ defaultByteBuffer)
      case r @ (Error(e), io) ⇒ e match {
        case _: IOException ⇒
        case e: Throwable ⇒ debug(text.stackTraceToString(e))
      }
      case e ⇒ error("unhandled " + e)
    }
  }

  final def respond(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ channel.write(io.buffer, io ++ k, iohandler) }
  }

  private final val response = """HTTP/1.1 200 OK
Date: Mon, 10 Sep 2012 15:06:09 GMT
Content-Type: text/plain
Content-Length: 5
Connection: keep-alive

PONG!""".getBytes
}

