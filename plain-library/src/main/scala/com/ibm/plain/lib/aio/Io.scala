package com.ibm.plain

package lib

package aio

import language.implicitConversions

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset

import scala.util.continuations.{ reset, shift, suspendable }
import scala.annotation.tailrec
import scala.math.min

import akka.util.ByteString

import text.{ ASCII, UTF8 }

import logging.HasLogger

import aio.Input.{ Elem, Eof, Empty }

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final case class Io(

  server: ServerChannel,

  channel: Channel,

  buffer: ByteBuffer,

  bytestring: ByteString,

  iteratee: Iteratee[Io, _],

  k: Io.IoHandler,

  n: Int,

  expected: Long) {

  import Io._

  def ++(server: ServerChannel) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(channel: Channel) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(buffer: ByteBuffer) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(bytestring: ByteString) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(iteratee: Iteratee[Io, _]) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(k: IoHandler) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(n: Int) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  def ++(expected: Long) = Io(server, channel, buffer, bytestring, iteratee, k, n, expected)

  /**
   * The Io 'that' always reflects the most current state, therefore it returns that ++ (this.bytestring + that.bytestring)
   */
  def ++(that: Io): Io = that ++ (this.bytestring ++ that.bytestring)

}

/**
 * The Io object contains all the complex continuations stuff.
 */
object Io

  extends HasLogger {

  /**
   * Helpers
   */
  final val empty = Io(null, null, null, ByteString.empty, null, null, -1, -1)

  type IoHandler = Io ⇒ Unit

  implicit def io2bytestring(io: Io): ByteString = io.bytestring

  @inline final def decode(io: Io)(implicit cset: Charset): String = io.bytestring.decodeString(cset.toString)

  @inline final def decode(bytestring: ByteString)(implicit cset: Charset): String = bytestring.decodeString(cset.toString)

  /**
   * Aio handling.
   */
  private[this] val accepthandler = new Handler[Channel, Io] {

    def completed(c: Channel, io: Io) = try {
      import io._
      server.accept(io, this)
      k(io ++ c ++ defaultByteBuffer)
    } catch {
      case _: java.io.EOFException ⇒
      case e: Throwable ⇒
        warning("accept error : " + e)
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

    def completed(count: Integer, io: Io) = try {
      import io._
      if (-1 < count) buffer.flip else channel.close
      k(io ++ count)
    } catch {
      case _: java.io.EOFException ⇒
      case e: Throwable ⇒
        warning("iohandler error : " + e)
        e.printStackTrace
        io.channel.close
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
      case io if -1 < io.n ⇒ io.iteratee(Elem(io ++ ByteString(io.buffer)))
      case io ⇒ io.iteratee(Eof)
    }) match {
      case (cont @ Cont(_), Empty) ⇒
        warning("need to read more")
        handle(io ++ cont)
      case (e @ Done(a), el @ Elem(io)) ⇒
        //        println(a)
        // dispatch here, dispatched request will eventually respond
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

