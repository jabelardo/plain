package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler, AsynchronousChannel, AsynchronousByteChannel }
import java.nio.ByteBuffer

/**
 * Common wrapper around an AsynchronousByteChannel
 */
abstract sealed class AsynchronousReadWriteChannel protected (

  channel: AsynchronousByteChannel)

  extends AsynchronousChannel {

  import AsynchronousReadWriteChannel._

  final def close = channel.close

  final def isOpen = channel.isOpen

}

final class AsynchronousReadChannel private (

  channel: AsynchronousByteChannel)

  extends AsynchronousReadWriteChannel(channel) {

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.read(buffer, attachment, handler)
  }

}

final class AsynchronousWriteChannel private (

  channel: AsynchronousByteChannel)

  extends AsynchronousReadWriteChannel(channel) {

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.write(buffer, attachment, handler)
  }

}

/**
 * Common things.
 */
private object AsynchronousReadWriteChannel {

  type Integer = java.lang.Integer

}

/**
 * A read-only wrapper around an AsynchronousByteChannel.
 */
object AsynchronousReadChannel {

  def apply(channel: AsynchronousByteChannel) = wrap(channel)

  def wrap(channel: AsynchronousByteChannel) = new AsynchronousReadChannel(channel)

}

/**
 * A write-only wrapper around an AsynchronousByteChannel.
 */
object AsynchronousWriteChannel {

  def apply(channel: AsynchronousByteChannel) = wrap(channel)

  def wrap(channel: AsynchronousByteChannel) = new AsynchronousWriteChannel(channel)

}

