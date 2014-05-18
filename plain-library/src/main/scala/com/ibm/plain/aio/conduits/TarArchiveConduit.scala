package com.ibm

package plain

package aio

package conduits

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files.createDirectories

import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }

import scala.{ Left, Right }
import scala.math.min

import io.{ ByteArrayInputStream, ByteBufferInputStream }

/**
 *
 */
final class TarArchiveConduit private (

  protected[this] final val directory: File,

  protected[this] final val purge: Boolean)

  extends TarArchiveSourceConduit

  with TarArchiveSinkConduit {

}

/**
 *
 */
object TarArchiveConduit {

  final def apply(directory: File, purge: Boolean) = new TarArchiveConduit(directory, purge)

}

/**
 * Create a SourceConduit from a source directory with all its content in the tar archive format to read from.
 */
sealed trait TarArchiveSourceConduit

  extends TarArchiveConduitBase

  with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    unsupported
  }

}

/**
 * Create a SinkConduit to write into a destination directory all content from the tar archive that is written to this sink.
 */
sealed trait TarArchiveSinkConduit

  extends TarArchiveConduitBase

  with TerminatingSinkConduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isOpen) {
      if (writingFile) {
        fixedlengthconduit.write(buffer, attachment, new TarArchiveSinkHandler(handler))
      } else {
        entry = nextEntry(buffer)
        if (null == entry) {
          if (isEof) {
            close
          }
        } else {
          if (entry.isDirectory) {
            nextDirectory
          } else if (entry.isFile) {
            nextFile
          } else {
            unsupported
          }
        }
        handler.completed(entrysize, attachment)
      }
    } else {
      handler.completed(0, attachment)
    }
  }

  private[this] final class TarArchiveSinkHandler[A](

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 == processed) {
        fixedlengthconduit.close
        fixedlengthconduit = null
        if (0 < padsize) {
          fixedlengthconduit = FixedLengthConduit(NullConduit, padsize)
          padsize = 0
        }
      }
      handler.completed(processed, attachment)
    }

  }

  private[this] final def nextEntry(buffer: ByteBuffer): TarArchiveEntry = {
    val e: Int = if (0 < underflow) {
      Array.copy(buffer.array, buffer.position, array, underflow, min(array.size - underflow, buffer.remaining))
      val b = new ByteArrayInputStream(array)
      val e = b.available
      in = new TarArchiveInputStream(b)
      entry = in.getNextTarEntry
      val len = (e - b.available) - underflow
      val used = -underflow
      buffer.position(buffer.position + len)
      underflow = 0
      used
    } else {
      in = new TarArchiveInputStream(new ByteBufferInputStream(buffer))
      if (0 == recordsize) recordsize = in.getRecordSize
      val e = buffer.position
      try entry = in.getNextTarEntry catch { case _: Throwable ⇒ entry = null }
      e
    }
    if (null == entry) {
      underflow = buffer.position - e
      if (null == array) array = new Array[Byte](4 * recordsize)
      Array.copy(buffer.array, e, array, 0, underflow)
      entrysize = underflow
      null
    } else {
      entrysize = buffer.position - e
      entry
    }
  }

  private[this] final def nextFile = {
    val size = entry.getSize
    padsize = (size % recordsize).toInt match { case 0 ⇒ 0 case e ⇒ recordsize - e }
    val filepath = directorypath.resolve(entry.getName)
    fixedlengthconduit = FixedLengthConduit(FileConduit.forWriting(filepath, size), size)
  }

  private[this] final def nextDirectory = {
    createDirectories(directorypath.resolve(entry.getName))
  }

  private[this] final def writingFile = null != fixedlengthconduit

  private[this] final def isEof = 2 * recordsize == entrysize

  private[this] final var entry: TarArchiveEntry = null

  private[this] final var in: TarArchiveInputStream = null

  private[this] final var fixedlengthconduit: FixedLengthConduit = null

  private[this] final var array: Array[Byte] = null

  private[this] final var underflow = 0

  private[this] final var entrysize = 0

  private[this] final var recordsize = 0

  private[this] final var padsize = 0

}

/**
 *
 */
sealed trait TarArchiveConduitBase

  extends Conduit {

  def close = isclosed = true

  def isOpen = !isclosed

  protected[this] val directory: File

  protected[this] val purge: Boolean

  protected[this] final val directorypath = {
    if (purge) {
      deleteDirectory(directory)
    }
    createDirectories(directory.toPath.toAbsolutePath)
  }

  private[this] final var isclosed = false

}
