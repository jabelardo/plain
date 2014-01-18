package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, AsynchronousFileChannel, CompletionHandler ⇒ Handler }
import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption.{ CREATE, READ, TRUNCATE_EXISTING, WRITE }

import scala.language.implicitConversions
import scala.collection.JavaConversions._

/**
 * Turns an AsynchronousFileChannel into an AsynchronousByteChannel to be used as source or destination for an AsynchronousChannelTransfer.
 */
final class FileByteChannel private (

  filechannel: AsynchronousFileChannel)

  extends AsynchronousByteChannel {

  private final type Integer = java.lang.Integer

  final def close = filechannel.close

  final def isOpen = filechannel.isOpen

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    filechannel.read(buffer, position, attachment, InnerCompletionHandler(handler))
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    filechannel.write(buffer, position, attachment, InnerCompletionHandler(handler))
  }

  /**
   * java.util.concurrent.Future is poorly implemented as it cannot be called asynchronously, therefore, these methods are not implemented.
   */
  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  override protected final def finalize = if (filechannel.isOpen) filechannel.close

  private[this] final class InnerCompletionHandler[A] private (

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = {
      position += processed
      outerhandler.completed(processed, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] object InnerCompletionHandler {

    final def apply[A](handler: Handler[Integer, _ >: A]): Handler[Integer, _ >: A] = new InnerCompletionHandler[A](handler)

  }

  private[this] final var position = 0L

}

/**
 *
 */
object FileByteChannel {

  implicit def asynchronousFileChannel2FileByteChannel(channel: AsynchronousFileChannel) = wrap(channel)

  final def wrap(filechannel: AsynchronousFileChannel): AsynchronousByteChannel = new FileByteChannel(filechannel)

  final def apply(filechannel: AsynchronousFileChannel): AsynchronousByteChannel = wrap(filechannel)

  final def forReading(path: Path): AsynchronousByteChannel = AsynchronousFileChannel.open(path, Set(READ), concurrent.ioexecutor)

  final def forWriting(path: Path): AsynchronousByteChannel = AsynchronousFileChannel.open(path, Set(CREATE, TRUNCATE_EXISTING, WRITE), concurrent.ioexecutor)

  /**
   * This is very fast and should, therefore, be preferred, it also fails if there is not enough space in the file system.
   */
  final def forWriting(path: Path, length: Long): AsynchronousByteChannel = {
    val f = new java.io.RandomAccessFile(path.toString, "rw")
    f.setLength(length)
    f.close
    AsynchronousFileChannel.open(path, Set(WRITE), concurrent.ioexecutor)
  }

  final def forReading(path: String): AsynchronousByteChannel = forReading(Paths.get(path))

  final def forWriting(path: String): AsynchronousByteChannel = forWriting(Paths.get(path))

  final def forWriting(path: String, length: Long): AsynchronousByteChannel = forWriting(Paths.get(path), length)

}

