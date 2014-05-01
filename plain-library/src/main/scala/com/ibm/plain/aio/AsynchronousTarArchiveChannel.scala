package com.ibm

package plain

package aio

import java.io.{ File, FileOutputStream, OutputStream }
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel, CompletionHandler ⇒ Handler }
import java.nio.file.{ Path, Paths, Files }

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.compress.archivers.tar._

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions
import scala.annotation.tailrec

import io.{ ByteBufferOutputStream, temporaryFile }

/**
 * Turns a folder of files into an AsynchronousByteChannel to be used as read-only(!) for an asynchronous transfer.
 */
final class AsynchronousTarArchiveChannel private (

  folder: Path)

  extends AsynchronousByteChannel {

  import AsynchronousTarArchiveChannel._

  private final type Integer = java.lang.Integer

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]): Unit = currentfilechannel match {
    case null if 0 > totalperfile ⇒
      handler.completed(-1, attachment)
    case null ⇒
      buffer.put(nullarray, 0, EofSize)
      totalperfile = -1
      handler.completed(buffer.position, attachment)
    case _ ⇒
      currentfilechannel.read(buffer, attachment, InnerCompletionHandler(buffer, handler))
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = unsupported

  final def read(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

  final def write(buffer: ByteBuffer): java.util.concurrent.Future[Integer] = unsupported

  final def close = directoriestmpfile.delete

  final def isOpen = true

  private[this] final class InnerCompletionHandler[A] private (

    buffer: ByteBuffer,

    outerhandler: Handler[Integer, _ >: A])

    extends Handler[Integer, A] {

    @inline def completed(processed: Integer, attachment: A) = {
      if (0 > processed && null != currentfilechannel) {
        val padsize = (totalperfile % tarrecordsize).toInt match { case 0 ⇒ 0 case e ⇒ tarrecordsize - e }
        if (0 < padsize) {
          buffer.put(nullarray, 0, padsize)
        }
        if (files.hasNext) {
          val file = files.next
          println("next entry " + file)
          tarArchive(new ByteBufferOutputStream(buffer)).putArchiveEntry(new TarArchiveEntry(file, relativePath(file)))
          totalperfile = 0L
          currentfilechannel = AsynchronousFileByteChannel.forReading(file)
          currentfilechannel.read(buffer, attachment, this)
        } else {
          currentfilechannel = null
          read(buffer, attachment, outerhandler)
        }
      } else {
        totalperfile += processed
        outerhandler.completed(processed, attachment)
      }
    }

    @inline def failed(e: Throwable, attachment: A) = outerhandler.failed(e, attachment)

  }

  private[this] object InnerCompletionHandler {

    final def apply[A](buffer: ByteBuffer, outerhandler: Handler[Integer, _ >: A]) = new InnerCompletionHandler[A](buffer, outerhandler)

  }

  private[this] final lazy val (files, directories) = {
    val f = new ArrayBuffer[File]
    def findFiles(file: File): Unit = {
      f += file
      if (file.isDirectory) file.listFiles.foreach(findFiles)
    }
    findFiles(folder.toFile)
    (f.filter(_.isFile).toIterator, f.filter(_.isDirectory).toList)
  }

  /**
   * Tweak TarArchive to be fit for long filenames and large files.
   */
  private[this] final lazy val (tardirectories, tarrecordsize): (AsynchronousFileByteChannel, Int) = {
    val fileout = new FileOutputStream(directoriestmpfile)
    val out = tarArchive(fileout)
    val recordsize = out.getRecordSize
    if (0 < directories.size) {
      directories.foreach { directory ⇒
        println("next directory " + directory)
        out.putArchiveEntry(new TarArchiveEntry(directory, relativePath(directory)))
        out.closeArchiveEntry
      }
      fileout.close
      (AsynchronousFileByteChannel.forReading(directoriestmpfile.toPath), recordsize)
    } else {
      out.close
      (null, recordsize)
    }
  }

  private[this] final def tarArchive(outputstream: OutputStream): TarArchiveOutputStream = {
    val out = new TarArchiveOutputStream(outputstream)
    out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
    out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    out.setAddPaxHeadersForNonAsciiNames(true)
    out
  }

  @inline private[this] final def relativePath(file: File) = {
    folder.getFileName + "/" + folder.relativize(file.toPath).toString
  }

  private[this] final lazy val directoriestmpfile = temporaryFile

  private[this] final var currentfilechannel: AsynchronousFileByteChannel = {
    if (null == tardirectories) {
      if (files.hasNext) {
        val file = files.next
        AsynchronousFileByteChannel.forReading(file)
      } else {
        null
      }
    } else {
      tardirectories
    }
  }

  private[this] final var totalperfile = 0L

}

/**
 *
 */
object AsynchronousTarArchiveChannel {

  final def apply(folder: Path): AsynchronousTarArchiveChannel = new AsynchronousTarArchiveChannel(folder)

  final def apply(folder: String): AsynchronousTarArchiveChannel = new AsynchronousTarArchiveChannel(Paths.get(folder))

  private final val EofSize = 1024

  private final val nullarray = Array.fill(EofSize)(0.toByte)

}

