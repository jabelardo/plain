package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, AsynchronousFileChannel, CompletionHandler }
import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption.{ CREATE, READ, WRITE }

import scala.language.implicitConversions
import scala.collection.JavaConversions._

/**
 * Turns an AsynchronousFileChannel into an AsynchronousByteChannel to be used as source or destination for an AsynchronousChannelTransfer.
 */
final class FileByteChannel private (

  filechannel: AsynchronousFileChannel)

  extends AsynchronousByteChannel {

  import FileByteChannel._

  type Integer = java.lang.Integer

  override protected def finalize = if (filechannel.isOpen) filechannel.close

  final def close = filechannel.close

  final def isOpen = filechannel.isOpen

  final def read[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.read(buffer, position, attachment, new InnerCompletionHandler[A](handler))
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, _ >: A]) = {
    filechannel.write(buffer, position, attachment, new InnerCompletionHandler[A](handler))
  }

  /**
   * java.util.concurrent.Future is poorly implemented as it cannot be called asynchronously, therefore, these methods are not implemented.
   */
  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  private[this] final class InnerCompletionHandler[A](

    outerhandler: CompletionHandler[Integer, _ >: A])

    extends CompletionHandler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = {
      position += processed
      outerhandler.completed(processed, attachment)
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

  def forReading(path: Path) = AsynchronousFileChannel.open(path, Set(READ), concurrent.ioexecutor)

  def forWriting(path: Path) = AsynchronousFileChannel.open(path, Set(CREATE, WRITE), concurrent.ioexecutor)

  implicit def asynchronousFileChannel2FileByteChannel(channel: AsynchronousFileChannel) = wrap(channel)

}

