package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

/**
 *
 */
final class FixedLengthChannel private (

  channel: Channel,

  offset: Long,

  length: Long)

  extends Channel {

  type Integer = java.lang.Integer

  final def close = channel.close

  final def isOpen = channel.isOpen

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    if (position < length) channel.read(buffer, attachment, new InnerCompletionHandler[A](handler)) else handler.completed(0, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.write(buffer, attachment, new InnerCompletionHandler[A](handler))
  }

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  private[this] final class InnerCompletionHandler[A](

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = {
      position += processed
      outerhandler.completed(processed, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] var position = offset

}

/**
 *
 */
object FixedLengthChannel {

  def apply(channel: Channel, offset: Long, length: Long) = new FixedLengthChannel(channel, offset, length)

}

