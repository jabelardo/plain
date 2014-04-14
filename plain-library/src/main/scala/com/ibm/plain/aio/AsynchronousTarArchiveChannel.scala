package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, CompletionHandler ⇒ Handler }
import java.nio.file.{ Path, Paths }

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.compress.archivers.tar._

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions
import scala.annotation.tailrec

/**
 * Turns a folder of files into an AsynchronousByteChannel to be used as read-only(!) for an asynchronous transfer.
 */
final class AsynchronousTarArchiveChannel private (

  folder: Path)

  extends AsynchronousByteChannel {

  private final type Integer = java.lang.Integer

  final val length: Long = -1

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = throw FutureNotSupported

  final def close = ()

  final def isOpen = true

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = unsupported

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

  // private[this] final val files = FileUtils.listFilesAndDirs(folder.toFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE).toList

  private[this] final var position = 0L

  {

    //   println("TarFile " + length + " " + files.size + " dirs " + files.filter(_.isDirectory).size)
    val out = new TarArchiveOutputStream(new java.io.FileOutputStream("/tmp/test2/test2.tar"), 64 * 1024)
    out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
    out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

    def addFiles(file: java.io.File): Unit = {
      //      println(folder.relativize(file.toPath))
      out.putArchiveEntry(new TarArchiveEntry(file, folder.relativize(file.toPath).toString))
      if (file.isFile) {
        java.nio.file.Files.copy(file.toPath, out)
        out.closeArchiveEntry
      } else if (file.isDirectory) {
        out.closeArchiveEntry
        file.listFiles.foreach(addFiles)
      }
    }

    addFiles(folder.toFile)
    out.close
  }

}

/**
 *
 */
object AsynchronousTarArchiveChannel {

  final def apply(folder: Path): AsynchronousTarArchiveChannel = new AsynchronousTarArchiveChannel(folder)

  final def apply(folder: String): AsynchronousTarArchiveChannel = new AsynchronousTarArchiveChannel(Paths.get(folder))

}

