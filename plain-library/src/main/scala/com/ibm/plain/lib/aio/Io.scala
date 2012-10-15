package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset

import scala.util.continuations.{ reset, shift, suspendable }
import scala.annotation.tailrec

import text.{ ASCII, UTF8 }

import logging.HasLogger

import aio.Input.{ Elem, Eof, Empty }

/**
 * Io represents the context of an asynchronous i/o operation.
 */
case class Io(

  server: ServerChannel,

  channel: Channel,

  buffer: ByteBuffer,

  iteratee: Iteratee[Io, _],

  k: Io.IoHandler,

  n: Int,

  expected: Long) {

  import Io._

  def ++(server: ServerChannel) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(channel: Channel) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(buffer: ByteBuffer) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(iteratee: Iteratee[Io, _]) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(k: IoHandler) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(n: Int) = Io(server, channel, buffer, iteratee, k, n, expected)

  def ++(expected: Long) = Io(server, channel, buffer, iteratee, k, n, expected)

  override def toString = getClass.getSimpleName + "(" + buffer + " pos=" + position + " lim=" + limit + " rem=" + remaining + " len " + length + " mar=" + mark + ")"

  def ++(that: Io): Io = try {
    if (0 == this.length)
      that
    else if (0 == that.length)
      this
    else {
      logging.defaultLogger.warning("Avoid this by enlarging buffersize " + this + " " + that)
      this.reset(null)
      val len = this.length + that.length
      val b = ByteBuffer.allocate(len)
      val aa = this.readBytes; logging.defaultLogger.info("aa<" + new String(aa) + ">")
      val bb = that.readBytes; logging.defaultLogger.info("bb<" + new String(bb) + ">")
      b.put(aa)
      b.put(bb)
      b.flip
      // releaseByteBuffer(this.buffer)
      //    releaseByteBuffer(that.buffer)
      val a = new Array[Byte](b.limit); b.get(a); b.rewind; println("buf<" + new String(a) + ">")
      logging.defaultLogger.info("after ++ " + this + " " + that)
      this ++ b
    }
  } catch { case e: Throwable ⇒ e.printStackTrace; throw e }

  /**
   * Do not call "relative" get or put methods on buffer: ByteBuffer for they would change it's internal state.
   */
  final def length = limit - position

  final def remaining = buffer.limit - position

  // remaining + length = buffer.limit - limit + limit - position = buffer.limit - position

  def take(n: Int): Io = {
    limit = position + n
    invariants
    this
  }

  def takeWhile(p: Int ⇒ Boolean): Io = {
    var pos = position
    while (pos < limit && p(buffer.get(pos))) pos += 1
    limit = pos
    invariants
    this
  }

  def takeUntil(delimiter: Byte): Io = takeWhile(_ != delimiter)

  /**
   * This is mainly implemented for `\r\n`.
   */
  def takeUntil(delimiter: Array[Byte]): Io = {
    reset(null)
    require(3 > delimiter.length, "Only implemented for delimiter.length < 3")
    delimiter.length match {
      case 0 ⇒ limit = position
      case 1 ⇒ takeWhile(_ != delimiter(0))
      case 2 ⇒
        var pos = position
        val a = delimiter(0)
        val b = delimiter(1)
        while (pos + 1 < limit && a != buffer.get(pos) && b != buffer.get(pos + 1)) pos += 1
        limit = pos
    }
    invariants
    this
  }

  def peek(n: Int): Io = {
    mark = position
    limit = position + n
    invariants
    this
  }

  def drop(n: Int): Io = {
    reset(null)
    position += n
    invariants
    this
  }

  def dropWhile(p: Byte ⇒ Boolean): Io = {
    while (p(buffer.get(position)) && position < limit) position += 1
    this
  }

  def decode(cset: Charset): String = reset(new String(readBytes, cset))

  def reset = buffer.reset

  private def readBytes: Array[Byte] = Array.fill(length)(readByte)

  private[this] def readByte: Byte = buffer.get(getAndIncrement)

  @inline private def reset[T](t: T): T = { limit = buffer.limit; if (-1 < mark) { position = mark; mark = -1 }; invariants; t }

  @inline private[this] def getAndIncrement = {
    val p = position
    position += 1
    invariants
    p
  }

  private[this] def invariants = try {
    //   println(position + " " + limit + " " + length + " " + remaining + " " + buffer)
    //   val a = new Array[Byte](1024); for (i ← 0 until buffer.limit) a.update(i, buffer.get(i)); println("buffer<" + new String(a) + ">")
    require(position <= limit, "#1")
    require(0 <= remaining, "#2")
    require(0 <= position, "#3")
    require(0 <= length, "#4")
    require(limit <= buffer.limit, "#5")
    require(0 <= limit, "#6")
  } catch {
    case e: Throwable ⇒
      println("error " + position + " " + limit + " " + length + " " + remaining + " " + buffer); e.printStackTrace
  }

  private[this] var limit = buffer.limit

  private[this] var position = buffer.position

  private[this] var mark = -1

}

/**
 * The Io object contains all the complex continuations stuff.
 */
object Io

  extends HasLogger {

  final val empty = Io(null, null, ByteBuffer.wrap(new Array[Byte](0)), null, null, -1, -1)

  type IoHandler = Io ⇒ Unit

  private[this] val accepthandler = new Handler[Channel, Io] {

    def completed(c: Channel, io: Io) = {
      import io._
      server.accept(io, this)
      k(io ++ c ++ defaultByteBuffer)
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

    def completed(count: Integer, io: Io) = {
      import io._
      if (-1 < count) buffer.flip else channel.close
      k(io ++ count)
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      channel.close
      warning("iohandler.failed " + e)
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
      case io if -1 < io.n ⇒ io.iteratee(Elem(io))
      case io ⇒ io.iteratee(Eof)
    }) match {
      case (cont @ Cont(_), Empty) ⇒
        warning("need to read more")
        handle(io ++ cont ++ defaultByteBuffer)
      case (e @ Done(a), Elem(io)) ⇒
        println(a)
        releaseByteBuffer(io.buffer)
        respond(io ++ ByteBuffer.wrap(response))
        handle(io ++ defaultByteBuffer)
      case (e, io) ⇒
        error("not handled: " + e + " " + io)
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

