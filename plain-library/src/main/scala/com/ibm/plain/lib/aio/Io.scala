package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel, CompletionHandler ⇒ Handler }

import scala.util.continuations.{ reset, shift, suspendable }

import logging.HasLogger

/**
 * Io represents the context of an asynchronous i/o operation.
 */
case class Io(

  server: ServerChannel,

  channel: Channel,

  buffer: ByteBuffer,

  iter: Iteratee[ByteBufferInput, _],

  k: Io.IoHandler,

  n: Int,

  expected: Long) {

  import Io._

  def ++(server: ServerChannel) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(channel: Channel) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(buffer: ByteBuffer) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(k: IoHandler) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(iter: Iteratee[ByteBufferInput, _]) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(n: Int) = Io(server, channel, buffer, iter, k, n, expected)

  def ++(expected: Long) = Io(server, channel, buffer, iter, k, n, expected)

}

object Io

  extends HasLogger {

  type IoHandler = Io ⇒ Unit

  private final val empty = Io(null, null, null, null, null, -1, -1)

  private[this] val accepthandler = new Handler[Channel, Io] {

    def completed(c: Channel, io: Io) = {
      import io._
      server.accept(io, this)
      k(io ++ c ++ ByteBuffer.allocateDirect(defaultBufferSize))
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
      k(io ++ Error[ByteBufferInput](e))
    }

  }

  def accept(server: ServerChannel): Io @suspendable =
    shift { k: IoHandler ⇒ server.accept(Io.empty ++ server ++ k, accepthandler) }

  def read(io: Io): Io @suspendable = {
    import io._
    shift { k: IoHandler ⇒ buffer.clear; channel.read(buffer, io ++ k, iohandler) }
  }

}

