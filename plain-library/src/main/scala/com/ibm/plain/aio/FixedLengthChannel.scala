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

  length: Long)

  extends Channel {

  println("fixedlength " + length)

  type Integer = java.lang.Integer

  final def close = channel.close

  final def isOpen = channel.isOpen

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.read(buffer, attachment, new InnerCompletionHandler[A](handler))
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
      outerhandler.completed(if (0 < processed && position <= length) processed else -1, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] var position = 0L

}

/**
 *
 */
object FixedLengthChannel {

  def apply(channel: Channel, length: Long) = new FixedLengthChannel(channel, length)

}

