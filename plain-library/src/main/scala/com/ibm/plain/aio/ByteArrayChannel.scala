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

  private final type Integer = java.lang.Integer

  final def close = total = 0

  final def isOpen = 0 < total

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    if (0 < total) {
      val len = scala.math.min(total, buffer.remaining)
      buffer.put(array, position, len)
      total -= len
      position += len
      handler.completed(len, attachment)
    } else handler.completed(0, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = unsupported

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

  private[this] final var position = offset

  private[this] final var total = length

}

/**
 *
 */
object ByteArrayChannel {

  final def apply(array: Array[Byte], offset: Int, length: Int) = new ByteArrayChannel(array, offset, length)

  final def apply(array: Array[Byte]): ByteArrayChannel = apply(array, 0, array.length)

}

