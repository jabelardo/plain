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

  implicit def asynchronousFileChannel2FileByteChannel(channel: AsynchronousFileChannel) = wrap(channel)

  def wrap(filechannel: AsynchronousFileChannel): AsynchronousByteChannel = new FileByteChannel(filechannel)

  def apply(filechannel: AsynchronousFileChannel): AsynchronousByteChannel = wrap(filechannel)

  def forReading(path: Path): AsynchronousByteChannel = AsynchronousFileChannel.open(path, Set(READ), concurrent.ioexecutor)

  def forReading(path: String): AsynchronousByteChannel = AsynchronousFileChannel.open(Paths.get(path), Set(READ), concurrent.ioexecutor)

  def forWriting(path: Path): AsynchronousByteChannel = AsynchronousFileChannel.open(path, Set(CREATE, WRITE), concurrent.ioexecutor)

  def forWriting(path: String): AsynchronousByteChannel = AsynchronousFileChannel.open(Paths.get(path), Set(CREATE, WRITE), concurrent.ioexecutor)

}

