package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

/**
 * Limit a channel to an offset and a length.
 */
final class FixedLengthConduit(

  protected[this] final val underlyingchannel: Channel,

  private[this] final val offset: Long,

  private[this] final val length: Long)

  extends Conduit[Channel] {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (position < length) underlyingchannel.read(buffer, attachment, new FixedLengthHandler(handler)) else handler.completed(0, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (position < length) underlyingchannel.write(buffer, attachment, new FixedLengthHandler(handler)) else handler.completed(0, attachment)
  }

  private[this] final class FixedLengthHandler[A](

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    final def completed(processed: Integer, attachment: A) = {
      position += processed
      handler.completed(processed, attachment)
    }

  }

  private[this] final var position = offset

}

object FixedLengthConduit {

  final def apply(underlyingchannel: Channel, offset: Long, length: Long) = new FixedLengthConduit(underlyingchannel, offset, length)

}