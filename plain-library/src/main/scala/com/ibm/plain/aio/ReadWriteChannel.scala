package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler, AsynchronousChannel, AsynchronousByteChannel }
import java.nio.ByteBuffer

/**
 * Common wrapper around an AsynchronousByteChannel
 */
abstract sealed class ReadWriteChannel protected (

  channel: AsynchronousByteChannel,

  canclose: Boolean)

  extends AsynchronousChannel {

  type Integer = java.lang.Integer

  final def close = if (canclose) channel.close

  final def isOpen = channel.isOpen

}

/**
 * A read-only channel.
 */
final class ReadChannel private (

  channel: AsynchronousByteChannel,

  canclose: Boolean)

  extends ReadWriteChannel(channel, canclose) {

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.read(buffer, attachment, handler)
  }

}

/**
 * A write-only channel.
 */
final class WriteChannel private (

  channel: AsynchronousByteChannel,

  canclose: Boolean)

  extends ReadWriteChannel(channel, canclose) {

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.write(buffer, attachment, handler)
  }

}

/**
 * A read-only wrapper around an AsynchronousByteChannel.
 */
object ReadChannel {

  def apply(channel: AsynchronousByteChannel, canclose: Boolean) = wrap(channel, canclose)

  def wrap(channel: AsynchronousByteChannel, canclose: Boolean) = new ReadChannel(channel, canclose)

}

/**
 * A write-only wrapper around an AsynchronousByteChannel.
 */
object WriteChannel {

  def apply(channel: AsynchronousByteChannel, canclose: Boolean) = wrap(channel, canclose)

  def wrap(channel: AsynchronousByteChannel, canclose: Boolean) = new WriteChannel(channel, canclose)

}

