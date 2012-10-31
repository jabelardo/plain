package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler, AsynchronousChannel, AsynchronousByteChannel }
import java.nio.ByteBuffer

import scala.language.implicitConversions

/**
 * Common wrapper around an AsynchronousByteChannel
 */
abstract sealed class ReadWriteChannel protected (

  channel: AsynchronousByteChannel)

  extends AsynchronousChannel {

  type Integer = java.lang.Integer

  final def close = channel.close

  final def isOpen = channel.isOpen

}

/**
 * A read-only channel.
 */
final class ReadChannel private (

  channel: AsynchronousByteChannel)

  extends ReadWriteChannel(channel) {

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.read(buffer, attachment, handler)
  }

}

/**
 * A write-only channel.
 */
final class WriteChannel private (

  channel: AsynchronousByteChannel)

  extends ReadWriteChannel(channel) {

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    channel.write(buffer, attachment, handler)
  }

}

/**
 * A read-only wrapper around an AsynchronousByteChannel.
 */
object ReadChannel {

  def apply(channel: AsynchronousByteChannel) = wrap(channel)

  def wrap(channel: AsynchronousByteChannel) = new ReadChannel(channel)

  implicit def asynchronousByteChannel2ReadChannel(channel: AsynchronousByteChannel) = wrap(channel)

}

/**
 * A write-only wrapper around an AsynchronousByteChannel.
 */
object WriteChannel {

  def apply(channel: AsynchronousByteChannel) = wrap(channel)

  def wrap(channel: AsynchronousByteChannel) = new WriteChannel(channel)

  implicit def asynchronousByteChannel2WriteChannel(channel: AsynchronousByteChannel) = wrap(channel)

}

