package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

import scala.math.min

/**
 * Wrap a Conduit around a ByteArray. Currently only supports reads.
 */
final class ByteArrayConduit(

  private[this] final val array: Array[Byte],

  private[this] final val offset: Int,

  private[this] final val length: Int)

  extends Conduit {

  override final def close = total = 0

  override final def isOpen = 0 < total

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (0 < total) {
      val len = min(total, buffer.remaining)
      buffer.put(array, position, len)
      total -= len
      position += len
      handler.completed(len, attachment)
    } else {
      handler.completed(0, attachment)
    }
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = unsupported

  protected[this] final val underlyingchannel: Channel = null

  private[this] final var position = offset

  private[this] final var total = length

}

object ByteArrayConduit {

  final def apply(array: Array[Byte], offset: Int, length: Int) = new ByteArrayConduit(array, offset, length)

  final def apply(array: Array[Byte]) = new ByteArrayConduit(array, 0, array.length)

}
