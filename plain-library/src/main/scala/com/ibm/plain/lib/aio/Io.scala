package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ Channel }

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

  @inline def ++(server: ServerChannel) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(channel: Channel) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(buffer: ByteBuffer) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(k: IoHandler) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(iter: Iteratee[ByteBufferInput, _]) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(n: Int) = Io(server, channel, buffer, iter, k, n, expected)

  @inline def ++(expected: Long) = Io(server, channel, buffer, iter, k, n, expected)

}

object Io {

  type IoHandler = Io ⇒ Unit

  val empty = Io(null, null, null, null, null, -1, -1)

}

