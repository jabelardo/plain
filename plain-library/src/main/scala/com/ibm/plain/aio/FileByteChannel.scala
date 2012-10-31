package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, AsynchronousFileChannel, CompletionHandler }
import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption.{ CREATE, READ, WRITE }

import scala.language.implicitConversions

/**
 * Turns an AsynchronousFileChannel into an AsynchronousByteChannel to be used as source or destination for an AsynchronousChannelTransfer.
 */
final class FileByteChannel private (

  filechannel: AsynchronousFileChannel)

  extends AsynchronousByteChannel {

  import FileByteChannel._

  type Integer = java.lang.Integer

  def close = filechannel.close

  def isOpen = filechannel.isOpen

  def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.read(buffer, readposition, attachment, new InnerCompletionHandler(true, handler))
  }

  def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.write(buffer, writeposition, attachment, new InnerCompletionHandler(false, handler))
  }

  /**
   * java.util.concurrent.Future is poorly implemented as it cannot be called asynchronously, therefore, these methods are not implemented.
   */
  def read(buffer: ByteBuffer) = throw FutureNotSupported

  /**
   * Same as read.
   */
  def write(buffer: ByteBuffer) = throw FutureNotSupported

  private[this] final class InnerCompletionHandler[A](

    read: Boolean,

    outerhandler: CompletionHandler[Integer, _ >: A])

    extends CompletionHandler[Integer, A] {

    @inline def completed(count: Integer, attachment: A) = {
      if (read) readposition += count.longValue else writeposition += count.longValue
      outerhandler.completed(count, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  @volatile private[this] var readposition = 0L

  @volatile private[this] var writeposition = 0L

}

/**
 *
 */
object FileByteChannel {

  def apply(filechannel: AsynchronousFileChannel) = wrap(filechannel)

  def wrap(filechannel: AsynchronousFileChannel) = new FileByteChannel(filechannel)

  def forReading(path: String): ReadChannel = forReading(Paths.get(path))

  def forWriting(path: String): WriteChannel = forWriting(Paths.get(path))

  def forReading(path: Path) = ReadChannel.wrap(AsynchronousFileChannel.open(path, READ))

  def forWriting(path: Path) = WriteChannel.wrap(AsynchronousFileChannel.open(path, CREATE, WRITE))

  implicit def asynchronousFileChannel2FileByteChannel(channel: AsynchronousFileChannel) = wrap(channel)

  implicit def fileByteChannel2ReadChannel(channel: FileByteChannel) = ReadChannel.wrap(channel)

  implicit def fileByteChannel2WriteChannel(channel: FileByteChannel) = WriteChannel.wrap(channel)

  private final val FutureNotSupported = new UnsupportedOperationException("Future not supported.")

}

