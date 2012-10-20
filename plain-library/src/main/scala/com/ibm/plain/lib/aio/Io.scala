package com.ibm.plain

package lib

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset

import scala.math.min
import scala.util.continuations.{ shift, suspendable }
import scala.concurrent.duration.Duration

import Iteratee.{ Cont, Done, Error }
import aio.Input.{ Elem, Empty, Eof }
import concurrent.{ OnlyOnce, scheduleOnce }
import logging.HasLogger

/**
 * Helper for Io with all low-level ByteBuffer methods.
 */
abstract sealed class IoHelper[E <: Io] {

  private[this] val self: E = this.asInstanceOf[E]

  import self._

  final def decode(implicit cset: Charset): String = resetBuffer(
    buffer.remaining match {
      case 1 ⇒ String.valueOf(buffer.get.toChar)
      case _ ⇒ new String(readBytes, cset)
    })

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
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

}

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final class Io private (

  var server: ServerChannel,

  val channel: Channel,

  var buffer: ByteBuffer,

  var iteratee: Iteratee[Io, _],

  var k: Io.IoHandler,

  var readwritten: Int,

  var expected: Long)

  extends IoHelper[Io] {

  import Io._

  @inline def ++(server: ServerChannel) = { this.server = server; this }

  @inline def ++(channel: Channel) = new Io(server, channel, buffer, iteratee, k, readwritten, expected)

  @inline def ++(buffer: ByteBuffer) = if (0 < this.buffer.remaining) {
    new Io(server, channel, buffer, iteratee, k, readwritten, expected)
  } else {
    this + buffer
  }

  @inline def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline def ++(k: IoHandler) = { this.k = k; this }

  @inline def ++(readwritten: Int) = { this.readwritten = readwritten; this }

  @inline def ++(expected: Long) = { this.expected = expected; this }

  @inline def releaseBuffer = {
    releaseByteBuffer(buffer)
    buffer = emptyBuffer
  }

  @inline private def +(buffer: ByteBuffer) = {
    releaseByteBuffer(this.buffer)
    this.buffer = buffer
    this
  }

  /**
   * The trick method of the entire algorithm, it should be called only when the buffer is too small and on start with Io.empty.
   */
  final def ++(that: Io): Io = if (0 == this.length) {
    that
  } else if (0 == that.length) {
    this
  } else {
    warnOnce
    val len = this.length + that.length
    val b = ByteBuffer.allocate(len)
    b.put(this.readBytes)
    this.releaseBuffer
    b.put(that.buffer)
    that.releaseBuffer
    b.flip
    that + b
  }

}

/**
 * The Io object contains all the complex continuations stuff.
 */
object Io

  extends HasLogger

  with OnlyOnce {

  import Iteratee._

  final private def warnOnce = onlyonce { warning("Chunked input found. Enlarge aio.default-buffer-size : " + defaultBufferSize) }

  final val emptyBuffer = ByteBuffer.allocate(0)

  final def empty = new Io(null, null, emptyBuffer, null, null, -1, -1)

  type IoHandler = Io ⇒ Unit

  /**
   * Aio handling.
   */
  private[this] final case class AcceptHandler(pauseinmilliseconds: Long)

    extends Handler[Channel, Io] {

    def completed(c: Channel, io: Io) = {
      import io._
      if (0 == pauseinmilliseconds)
        server.accept(io, this)
      else
        scheduleOnce(pauseinmilliseconds)(server.accept(io, this))
      k(io ++ c ++ defaultByteBuffer)
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      if (server.isOpen) {
        /**
         * Do not pause here, in case of failure we want to be back online asap.
         */
        server.accept(io, this)
        e match {
          case _: IOException ⇒
          case e: Throwable ⇒ warning("accept failed : " + io + " " + e)
        }
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

  final def accept(server: ServerChannel, pausebetweenaccepts: Duration): Io @suspendable =
    shift { k: IoHandler ⇒ server.accept(Io.empty ++ server ++ k, AcceptHandler(pausebetweenaccepts.toMillis)) }

  private[this] final def read(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ channel.read(buffer, io ++ k, iohandler) }
  }

  private[this] final def respond(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ channel.write(io.buffer, io ++ k, iohandler) }
  }

  final def handle(io: Io): Unit @suspendable = {
    (read(io) match {
      case io if -1 < io.readwritten ⇒ io.iteratee(Elem(io))
      case io ⇒
        io.releaseBuffer
        io.iteratee(Eof)
    }) match {
      case (cont @ Cont(_), Empty) ⇒
        handle(io ++ cont ++ defaultByteBuffer)
      case (e @ Done(a), el @ Elem(io)) ⇒ // move handling/dispatching outside 
        // println(a)
        // println(io.buffer + " remaining " + io.buffer.remaining)
        io.releaseBuffer
        val r = defaultByteBuffer
        r.put(response)
        r.flip
        respond(io ++ r)
        io.releaseBuffer
        handle(io ++ defaultByteBuffer)
      case (Error(e), Elem(io)) if e.isInstanceOf[http.Status] ⇒ // move error handling outside
        println(e)
        io.releaseBuffer
        val r = defaultByteBuffer
        r.put(badrequest)
        r.flip
        respond(io ++ r)
        io.releaseBuffer
        io.channel.close
      case r @ (Error(e), Elem(io)) ⇒
        io.releaseBuffer
        io.channel.close
        e match {
          case _: IOException ⇒
          case e: Throwable ⇒ debug(text.stackTraceToString(e))
        }
      case (Error(_), Eof) ⇒
      case e ⇒ error("Unhandled : " + e)
    }
  }

  /**
   * testing
   */
  private final val response = "HTTP/1.1 200 OK\r\nDate: Mon, 10 Sep 2012 15:06:09 GMT\r\nContent-Type: text/plain\r\nContent-Length: 5\r\nConnection: keep-alive\r\n\r\nPONG!".getBytes

  private final val badrequest = "HTTP/1.1 400 Bad Request\r\nDate: Mon, 10 Sep 2012 15:06:09 GMT\r\nConnection: close\r\n\r\n".getBytes

}

