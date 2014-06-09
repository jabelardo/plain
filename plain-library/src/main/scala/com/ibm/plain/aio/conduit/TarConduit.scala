package com.ibm

package plain

package aio

package conduit

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files.createDirectories

import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream }

import scala.{ Left, Right }
import scala.collection.mutable.ListBuffer
import scala.math.min

import io.{ ByteArrayInputStream, ByteBufferInputStream, ByteBufferOutputStream }
import NullConduit.nul

/**
 *
 */
final class TarConduit private (

  protected[this] final val directory: File,

  protected[this] final val purge: Boolean)

  extends TarSourceConduit

  with TarSinkConduit

  with TerminatingConduit {

}

/**
 *
 */
object TarConduit {

  final def apply(directory: String, purge: Boolean) = new TarConduit(new File(directory), purge)

  final def apply(directory: File, purge: Boolean) = new TarConduit(directory, purge)

  final def apply(directory: File) = new TarConduit(directory, false)

}

/**
 * Create a SourceConduit from a source directory with all its content in the tar archive format to read from.
 */
sealed trait TarSourceConduit

  extends TarConduitBase

  with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isOpen) {
      if (reading) {
        fixedlengthconduit.read(buffer, attachment, new TarArchiveSourceHandler(buffer, handler))
      } else {
        if (!nextEntry(buffer)) {
          entrysize = 2 * recordsize
          buffer.put(nul, 0, entrysize)
          close
        }
        handler.completed(entrysize, attachment)
      }
    } else {
      handler.completed(0, attachment)
    }
  }

  private[this] final class TarArchiveSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 >= processed) {
        fixedlengthconduit.close
        fixedlengthconduit = null
        if (0 < padsize) {
          fixedlengthconduit = FixedLengthConduit(NullConduit, padsize - 1)
          padsize = 0
          buffer.put(0.toByte)
          handler.completed(1, attachment)
        } else {
          read(buffer, attachment, handler)
        }
      } else {
        handler.completed(processed, attachment)
      }
    }

  }

  private[this] final def nextEntry(buffer: ByteBuffer) = {
    if (files.hasNext) {
      val file = files.next
      if (null == out) nextOut(buffer)
      val e = buffer.position
      out.putArchiveEntry(new TarArchiveEntry(file, relativePath(file)))
      entrysize = buffer.position - e
      nextFile(file)
      true
    } else {
      false
    }
  }

  private[this] final def nextFile(file: File) = if (file.isFile) {
    val size = file.length
    padsize = (size % recordsize).toInt match { case 0 ⇒ 0 case e ⇒ recordsize - e }
    fixedlengthconduit = FixedLengthConduit(FileConduit.forReading(file), size)
    out = null
  }

  private[this] final def nextOut(buffer: ByteBuffer) = {
    out = new TarArchiveOutputStream(new ByteBufferOutputStream(buffer))
    out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
    out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
    out.setAddPaxHeadersForNonAsciiNames(true)
    if (0 == recordsize) recordsize = out.getRecordSize
  }

  private[this] final def reading = null != fixedlengthconduit

  private[this] final def relativePath(file: File) = {
    directorypath.getFileName + "/" + directorypath.relativize(file.toPath).toString
  }

  private[this] final var out: TarArchiveOutputStream = null

  private[this] final lazy val files = {
    val f = new ListBuffer[File]
    def findFiles(file: File): Unit = {
      f += file
      if (file.isDirectory) file.listFiles.foreach(findFiles)
    }
    findFiles(directory)
    f.toIterator
  }

}

/**
 * Create a SinkConduit to write into a destination directory all content from the tar archive that is written to this sink.
 */
sealed trait TarSinkConduit

  extends TarConduitBase

  with TerminatingSinkConduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (writing) {
      fixedlengthconduit.write(buffer, attachment, new TarArchiveSinkHandler(handler))
    } else {
      nextEntry(buffer)
      if (null == entry) {
        if (lastEntry) close
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
  }

  private[this] final class TarArchiveSinkHandler[A](

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 >= processed) {
        if (null != fixedlengthconduit) {
          fixedlengthconduit.close
          fixedlengthconduit = null
          if (0 < padsize) {
            fixedlengthconduit = FixedLengthConduit(NullConduit, padsize)
            padsize = 0
          }
        }
      }
      handler.completed(processed, attachment)
    }

  }

  private[this] final def nextEntry(buffer: ByteBuffer) = {
    if (0 < buffer.remaining) {
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
        if (0 == recordsize) {
          purgeDirectory
          recordsize = in.getRecordSize
        }
        val e = buffer.position
        try entry = in.getNextTarEntry catch { case _: Throwable ⇒ entry = null }
        e
      }
      if (null == entry) {
        underflow = buffer.position - e
        if (null == array) array = new Array[Byte](4 * recordsize)
        Array.copy(buffer.array, e, array, 0, underflow)
        entrysize = underflow
      } else {
        entrysize = buffer.position - e
      }
    }
  }

  private[this] final def nextFile = {
    val size = entry.getSize
    padsize = (size % recordsize).toInt match { case 0 ⇒ 0 case e ⇒ recordsize - e }
    fixedlengthconduit = FixedLengthConduit(FileConduit.forWriting(directorypath.resolve(entry.getName), size), size)
  }

  private[this] final def nextDirectory = {
    createDirectories(directorypath.resolve(entry.getName))
  }

  private[this] final def writing = null != fixedlengthconduit

  private[this] final def lastEntry = 2 * recordsize == entrysize

  private[this] final var entry: TarArchiveEntry = null

  private[this] final var in: TarArchiveInputStream = null

  private[this] final var array: Array[Byte] = null

  private[this] final var underflow = 0

}

/**
 *
 */
sealed trait TarConduitBase

  extends Conduit {

  def close = isclosed = true

  def isOpen = !isclosed

  protected[this] final def purgeDirectory = if (purge) deleteDirectory(directory)

  protected[this] val directory: File

  protected[this] val purge: Boolean

  protected[this] final val directorypath = createDirectories(directory.toPath.toAbsolutePath)

  protected[this] final var fixedlengthconduit: FixedLengthConduit = null

  protected[this] final var recordsize = 0

  protected[this] final var padsize = 0

  protected[this] final var entrysize = 0

  private[this] final var isclosed = false

}



