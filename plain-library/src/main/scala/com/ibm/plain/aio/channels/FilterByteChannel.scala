package com.ibm

package plain

package aio

package channels

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

/**
 *
 */
abstract class FilterByteChannel protected (

  underlyingchannel: Channel)

  extends Channel {

  def close = underlyingchannel.close

  def isOpen = underlyingchannel.isOpen

  final def read[A](outerbuffer: ByteBuffer, attachment: A, outerhandler: Handler[Integer, _ >: A]) = {
    if (0 < innerbuffer.remaining) {
      println("still read remaining " + innerbuffer.remaining)
      onReadComplete(attachment, outerbuffer, outerhandler, innerbuffer, innerbuffer.remaining)
    } else {
      println("need to read inner")
      innerbuffer.clear
      underlyingchannel.read(innerbuffer, attachment, ReadHandler(outerbuffer, outerhandler))
    }
  }

  final def write[A](outerbuffer: ByteBuffer, attachment: A, outerhandler: Handler[Integer, _ >: A]) = {
    if (0 < innerbuffer.remaining) {
    } else {
      innerbuffer.flip
      underlyingchannel.write(innerbuffer, attachment, WriteHandler(outerbuffer, outerhandler))
    }
  }

  /**
   * This is where the filter logic goes for Reads.
   */
  protected[this] def onReadComplete[A](

    attachment: A,

    outerbuffer: ByteBuffer,

    outerhandler: Handler[Integer, _ >: A],

    innerbuffer: ByteBuffer,

    innerprocessed: Integer)

  /**
   * This is where the filter logic goes for Writes.
   */
  protected[this] def onWriteComplete[A](

    attachment: A,

    outerbuffer: ByteBuffer,

    outerhandler: Handler[Integer, _ >: A],

    innerbuffer: ByteBuffer,

    innerprocessed: Integer)

  /**
   * Inner handlers.
   */
  private[this] final class ReadHandler[A] private (

    outerbuffer: ByteBuffer,

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline final def completed(innerprocessed: Integer, attachment: A) = {
      println("before flip inner " + format(innerbuffer))
      innerbuffer.flip
      onReadComplete(attachment, outerbuffer, outerhandler, innerbuffer, innerprocessed)
    }

    @inline final def failed(e: Throwable, attachment: A) = onReadFailed(e, attachment, outerhandler, innerbuffer)

  }

  private[this] final class WriteHandler[A] private (

    outerbuffer: ByteBuffer,

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline final def completed(innerprocessed: Integer, attachment: A) = onReadComplete(attachment, outerbuffer, outerhandler, innerbuffer, innerprocessed)

    @inline final def failed(e: Throwable, attachment: A) = onWriteFailed(e, attachment, outerhandler, innerbuffer)

  }

  protected[this] def onReadFailed[A](e: Throwable, attachment: A, outerhandler: Handler[Integer, _ >: A], innerbuffer: ByteBuffer) = outerhandler.failed(e, attachment)

  protected[this] def onWriteFailed[A](e: Throwable, attachment: A, outerhandler: Handler[Integer, _ >: A], innerbuffer: ByteBuffer) = outerhandler.failed(e, attachment)

  private[this] object ReadHandler { final def apply[A](buffer: ByteBuffer, handler: Handler[Integer, _ >: A]) = new ReadHandler(buffer, handler) }

  private[this] object WriteHandler { final def apply[A](buffer: ByteBuffer, handler: Handler[Integer, _ >: A]) = new WriteHandler(buffer, handler) }

  protected[this] final val innerbuffer = {
    val buffer = ByteBuffer.wrap(new Array[Byte](defaultBufferSize))
    buffer.flip
    buffer
  }

  /**
   * Not used.
   */
  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

}
