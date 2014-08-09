package com.ibm

package plain

package aio

package conduit

import java.io.{ File, RandomAccessFile }
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousFileChannel ⇒ FileChannel }
import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption.{ CREATE, READ, TRUNCATE_EXISTING, WRITE }

import scala.collection.JavaConversions.setAsJavaSet

/**
 * Converts an AsynchronousFileChannel into an AsynchronousByteChannel.
 */
final class FileConduit(

  protected[this] final val wrappedchannel: FileChannel)

    extends WrapperConduit

    with TerminatingConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    wrappedchannel.read(buffer, position, attachment, new FileHandler(handler))
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    wrappedchannel.write(buffer, position, attachment, new FileHandler(handler))
  }

  private[this] final class FileHandler[A](

    private[this] final val handler: Handler[A])

      extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 < processed) position += processed
      handler.completed(processed, attachment)
    }

  }

  private[this] final var position = 0L

}

object FileConduit {

  final def apply(filechannel: FileChannel) = new FileConduit(filechannel)

  final def forReading(path: Path): FileConduit = apply(FileChannel.open(path, Set(READ), concurrent.ioexecutor))

  final def forWriting(path: Path): FileConduit = apply(FileChannel.open(path, Set(CREATE, TRUNCATE_EXISTING, WRITE), concurrent.ioexecutor))

  /**
   * This is very fast and should, therefore, be preferred, it also fails if there is not enough space in the file system.
   */
  final def forWriting(path: Path, length: Long): FileConduit = if (0 < length) {
    val f = new RandomAccessFile(path.toString, "rw")
    f.setLength(length)
    f.close
    apply(FileChannel.open(path, Set(WRITE), concurrent.ioexecutor))
  } else {
    forWriting(path)
  }

  final def forReading(file: File): FileConduit = forReading(file.toPath)

  final def forReading(path: String): FileConduit = forReading(Paths.get(path))

  final def forWriting(file: File): FileConduit = forWriting(file.toPath)

  final def forWriting(path: String): FileConduit = forWriting(Paths.get(path))

  final def forWriting(file: File, length: Long): FileConduit = forWriting(file.toPath, length)

  final def forWriting(path: String, length: Long): FileConduit = forWriting(Paths.get(path), length)

}
