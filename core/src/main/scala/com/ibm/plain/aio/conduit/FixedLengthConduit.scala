package com.ibm

package plain

package aio

package conduit

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

/**
 * Limit a Conduit to an offset and a length.
 */
final class FixedLengthConduit(

  protected[this] final val underlyingchannel: Channel,

  private[this] final val length: Long)

  extends ConnectorConduit[Channel] {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (position < length) underlyingchannel.read(wrapper(buffer), attachment, new FixedLengthHandler(handler)) else handler.completed(0, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (position < length) underlyingchannel.write(wrapper(buffer), attachment, new FixedLengthHandler(handler)) else handler.completed(0, attachment)
  }

  private[this] final class FixedLengthHandler[A](

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    final def completed(processed: Integer, attachment: A) = {
      position += processed
      handler.completed(processed, attachment)
    }

  }

  private[this] final def wrapper(buffer: ByteBuffer) = if (buffer.remaining < length - position) {
    buffer
  } else {
    val len = (length - position).toInt
    val buf = ByteBuffer.wrap(buffer.array, buffer.position, len)
    buffer.position(buffer.position + len)
    buf
  }

  private[this] final var position = 0

}

/**
 *
 */
object FixedLengthConduit {

  final def apply(underlyingchannel: Channel, length: Long) = new FixedLengthConduit(underlyingchannel, length)

}