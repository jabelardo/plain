package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler, AsynchronousFileChannel, AsynchronousByteChannel }
import java.nio.ByteBuffer

/**
 *
 */
class AsynchronousFileByteChannel private (

  filechannel: AsynchronousFileChannel)

  extends AsynchronousByteChannel {

  import AsynchronousFileByteChannel._

  type Integer = java.lang.Integer

  def close = filechannel.close

  def isOpen = filechannel.isOpen

  def read(buffer: ByteBuffer) = throw futureNotSupported

  def write(buffer: ByteBuffer) = throw futureNotSupported

  def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.read(buffer, readposition, attachment, new InnerCompletionHandler(true, handler))
  }

  def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.write(buffer, writeposition, attachment, new InnerCompletionHandler(false, handler))
  }

  private[this] class InnerCompletionHandler[A](

    read: Boolean,

    outerhandler: CompletionHandler[Integer, _ >: A])

    extends CompletionHandler[Integer, A] {

    def completed(count: Integer, attachment: A) = {
      if (read) readposition += count.longValue else writeposition += count.longValue
      outerhandler.completed(count, attachment)
    }

    def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  @volatile private[this] var readposition = 0L

  @volatile private[this] var writeposition = 0L

}

/**
 *
 */
object AsynchronousFileByteChannel {

  def wrap(filechannel: AsynchronousFileChannel) = new AsynchronousFileByteChannel(filechannel)

  private final val futureNotSupported = new UnsupportedOperationException("Future not supported.")

}

