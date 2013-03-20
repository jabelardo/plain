package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

/**
 *
 */
final class ByteArrayChannel private (

  array: Array[Byte],

  offset: Int,

  length: Int)

  extends Channel {

  type Integer = java.lang.Integer

  final def close = total = 0

  final def isOpen = 0 < total

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    if (0 < total) {
      val len = scala.math.min(total, buffer.remaining)
      buffer.put(array, position, len)
      total -= len
      position += len
      handler.completed(len, attachment)
    } else handler.completed(0, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = throw new UnsupportedOperationException

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  private[this] var position = offset

  private[this] var total = length

}

/**
 *
 */
object ByteArrayChannel {

  def apply(array: Array[Byte], offset: Int, length: Int) = new ByteArrayChannel(array, offset, length)

  def apply(array: Array[Byte]): ByteArrayChannel = apply(array, 0, array.length)

}
