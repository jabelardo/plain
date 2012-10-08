package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }

import scala.util.continuations.{ reset, shift, suspendable }

import com.ibm.plain.lib.aio.Iteratee

import HttpIteratees.readRequestLine
import aio.{ ByteBufferInput, Cont, Error }
import aio.Input.{ Elem, Eof }

object HttpAio {

  import HttpIteratees._

  /**
   * async stuff, incredibly difficult
   */
  type Integer = java.lang.Integer

  type IoHandler = Io ⇒ Unit

  val accepthandler = new Handler[Channel, Io] {

    def completed(ch: Channel, io: Io) = {
      import io._
      server.accept(io, this)
      k(io ++ ch ++ ByteBuffer.allocateDirect(192))
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      if (server.isOpen) {
        server.accept(io, this)
        println(e)
      }
    }

  }

  val iohandler = new Handler[Integer, Io] {

    def completed(count: Integer, io: Io) = {
      import io._
      if (-1 < count) buffer.flip else channel.close
      k(io ++ count)
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      channel.close
      k(io ++ Error[ByteBufferInput](e))
    }

  }

  case class Io(server: ServerChannel, channel: Channel, buffer: ByteBuffer, it: Iteratee[ByteBufferInput, _], k: IoHandler, n: Int) {

    def ++(server: ServerChannel) = Io(server, channel, buffer, it, k, n)

    def ++(channel: Channel) = Io(server, channel, buffer, it, k, n)

    def ++(buffer: ByteBuffer) = Io(server, channel, buffer, it, k, n)

    def ++(k: IoHandler) = Io(server, channel, buffer, it, k, n)

    def ++(it: Iteratee[ByteBufferInput, _]) = Io(server, channel, buffer, it, k, n)

    def ++(n: Int) = Io(server, channel, buffer, it, k, n)

  }

  object Io {

    val empty = Io(null, null, null, null, null, -1)

  }

  def accept(server: ServerChannel): Io @suspendable =
    shift { k: IoHandler ⇒ server.accept(Io.empty ++ server ++ k, accepthandler) }

  def read(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ buffer.clear; channel.read(buffer, io ++ k, iohandler) }
  }

  def readRequest(io: Io, iter: Iteratee[ByteBufferInput, Any]): (Io, Any) @suspendable = {
    import io._
    shift { k: (((Io, Any)) ⇒ Unit) ⇒ k(io, iter) }
    val r = read(io)
    iter(if (-1 < r.n) Elem(ByteBufferInput(r.buffer)) else Eof) match {
      case (c @ Cont(_), _) ⇒ println("not enough"); readRequest(r ++ c, c)
      case (e, _) ⇒ (io, e)
    }
  }

  def readR(io: Io): Io @suspendable = {
    import io._
    val (i, req) = readRequest(io, readRequestLine)
    write(i)
  }

  def write(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ channel.write(ByteBuffer.wrap(response), io ++ k, iohandler) }
    readR(io ++ readRequestLine)
  }

  def test(server: ServerChannel) = {
    reset {
      readR(accept(server))
      println
    }

  }

  private[this] final val response = """HTTP/1.1 200 OK
Date: Mon, 10 Sep 2012 15:06:09 GMT
Content-Type: text/plain
Content-Length: 5
Connection: keep-alive

PONG!""".getBytes

}