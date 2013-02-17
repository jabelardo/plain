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

  override protected def finalize = if (filechannel.isOpen) filechannel.close

  def close = filechannel.close

  def isOpen = filechannel.isOpen

  def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.read(buffer, position, attachment, new InnerCompletionHandler(true, handler))
  }

  def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.write(buffer, position, attachment, new InnerCompletionHandler(false, handler))
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
      position += count
      outerhandler.completed(count, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] var position = 0L

}

/**
 *
 */
object FileByteChannel {

  def apply(filechannel: AsynchronousFileChannel) = wrap(filechannel)

  def wrap(filechannel: AsynchronousFileChannel) = new FileByteChannel(filechannel)

  def forReading(path: Path) = AsynchronousFileChannel.open(path, READ)

  def forWriting(path: Path) = AsynchronousFileChannel.open(path, CREATE, WRITE)

  implicit def asynchronousFileChannel2FileByteChannel(channel: AsynchronousFileChannel) = wrap(channel)

}

