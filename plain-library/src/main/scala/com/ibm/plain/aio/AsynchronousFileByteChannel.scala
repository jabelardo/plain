package com.ibm

package plain

package aio

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, AsynchronousFileChannel, CompletionHandler ⇒ Handler }
import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption.{ CREATE, READ, TRUNCATE_EXISTING, WRITE }

import scala.language.implicitConversions
import scala.collection.JavaConversions._

/**
 * Turns an AsynchronousFileChannel into an AsynchronousByteChannel to be used as source or destination for an asynchronous transfer.
 */
final class AsynchronousFileByteChannel private (

  filechannel: AsynchronousFileChannel)

  extends AsynchronousByteChannel {

  private final type Integer = java.lang.Integer

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  override protected final def finalize = if (filechannel.isOpen) filechannel.close

  final def close = filechannel.close

  final def isOpen = filechannel.isOpen

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    filechannel.read(buffer, position, attachment, InnerCompletionHandler(handler))
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    filechannel.write(buffer, position, attachment, InnerCompletionHandler(handler))
  }

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

  private[this] final class ReadCompletionHandler[A] private (

    buffer: ByteBuffer,

    source: AsynchronousByteChannel,

    target: AsynchronousByteChannel,

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = {
      buffer.flip
      target.write(buffer, attachment, writecompletionhandler)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

    final var writecompletionhandler: Handler[Integer, _ >: A] = null

  }

  private[this] object ReadCompletionHandler {

    final def apply[A](buffer: ByteBuffer, source: AsynchronousByteChannel, target: AsynchronousByteChannel, handler: Handler[Integer, _ >: A]) =
      new ReadCompletionHandler[A](buffer, source, target, handler)

  }

  private[this] final class WriteCompletionHandler[A] private (

    buffer: ByteBuffer,

    source: AsynchronousByteChannel,

    target: AsynchronousByteChannel,

    readcompletionhandler: Handler[Integer, _ >: A],

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = if (0 < processed) {
      position += processed
      buffer.clear
      source.read(buffer, attachment, readcompletionhandler)
    } else {
      outerhandler.completed(processed, attachment)
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] object WriteCompletionHandler {

    final def apply[A](buffer: ByteBuffer, source: AsynchronousByteChannel, target: AsynchronousByteChannel, readcompletionhandler: Handler[Integer, _ >: A], handler: Handler[Integer, _ >: A]) = {
      new WriteCompletionHandler[A](buffer, source, target, readcompletionhandler, handler)
    }

  }

  private[this] final var position = 0L

}

/**
 *
 */
object AsynchronousFileByteChannel {

  final def apply(filechannel: AsynchronousFileChannel): AsynchronousFileByteChannel = new AsynchronousFileByteChannel(filechannel)

  final def forReading(path: Path): AsynchronousFileByteChannel = apply(AsynchronousFileChannel.open(path, Set(READ), concurrent.ioexecutor))

  final def forWriting(path: Path): AsynchronousFileByteChannel = apply(AsynchronousFileChannel.open(path, Set(CREATE, TRUNCATE_EXISTING, WRITE), concurrent.ioexecutor))

  /**
   * This is very fast and should, therefore, be preferred, it also fails if there is not enough space in the file system.
   */
  final def forWriting(path: Path, length: Long): AsynchronousFileByteChannel = {
    val f = new java.io.RandomAccessFile(path.toString, "rw")
    f.setLength(length)
    f.close
    apply(AsynchronousFileChannel.open(path, Set(WRITE), concurrent.ioexecutor))
  }

  final def forReading(file: File): AsynchronousFileByteChannel = forReading(file.toPath)

  final def forReading(path: String): AsynchronousFileByteChannel = forReading(Paths.get(path))

  final def forWriting(file: File): AsynchronousFileByteChannel = forWriting(file.toPath)

  final def forWriting(path: String): AsynchronousFileByteChannel = forWriting(Paths.get(path))

  final def forWriting(file: File, length: Long): AsynchronousFileByteChannel = forWriting(file.toPath, length)

  final def forWriting(path: String, length: Long): AsynchronousFileByteChannel = forWriting(Paths.get(path), length)

}

