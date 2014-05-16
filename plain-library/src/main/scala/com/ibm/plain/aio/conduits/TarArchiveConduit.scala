package com.ibm

package plain

package aio

package conduits

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.nio.file.Files.createDirectories
import java.nio.file.Paths

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }

import io.ByteBufferInputStream

/**
 *
 */
final class TarArchiveConduit private (

  protected[this] final val directory: File)

  extends TarArchiveSourceConduit

  with TarArchiveSinkConduit {

}

/**
 *
 */
object TarArchiveConduit {

  final def apply(directory: File) = new TarArchiveConduit(directory)

}

/**
 * Create a SourceConduit from a source directory with all its content in the tar archive format to read from.
 */
sealed trait TarArchiveSourceConduit

  extends TarArchiveConduitBase

  with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {

  }

}

/**
 * Create a SinkConduit to write into a destination directory all content from the tar archive that is written to this sink.
 */
sealed trait TarArchiveSinkConduit

  extends TarArchiveConduitBase

  with TerminatingSinkConduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (writingFile) {
      fixedlengthconduit.write(buffer, attachment, new TarArchiveSinkHandler(handler))
    } else {

      if (null == in) {
        in = new TarArchiveInputStream(new ByteBufferInputStream(buffer))
        if (0 == recordsize) {
          recordsize = in.getRecordSize
          entrybuffer = bestFitByteBuffer(2 * recordsize)
        }
      }
      if (hasSufficient(buffer)) {
        val e = buffer.position
        val nextentry = in.getNextTarEntry
        if (null == nextentry) {
          println("eof")
        } else {
          if (nextentry.isDirectory) {
            println("directory " + nextentry.getName)
            createDirectories(directorypath.resolve(nextentry.getName))
          } else if (nextentry.isFile) {
            in.close
            in = null
            println("file " + nextentry.getName + " " + nextentry.getSize)
            val size = nextentry.getSize
            padsize = (size % recordsize).toInt match { case 0 ⇒ 0 case e ⇒ recordsize - e }
            val filepath = directorypath.resolve(nextentry.getName)
            fixedlengthconduit = FixedLengthConduit(FileConduit.forWriting(filepath, size), size)
          } else {
            unsupported
          }
        }
        handler.completed(buffer.position - e, attachment)
      } else {
        println("buffer not suff " + buffer)
        val e = buffer.remaining
        println("not sufficient " + e)
        Array.copy(buffer.array, buffer.position, buffer.array, 0, e)
        buffer.position(e)
        buffer.limit(buffer.capacity)
        println("buff after not suff " + buffer)
        in = null
        handler.completed(e, attachment)
      }
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

  private[this] final def hasSufficient(buffer: ByteBuffer) = if (0 < recordsize) 1 * recordsize <= buffer.remaining else 512 <= buffer.remaining

  /**
   * @return Left(TarArchiveEntry) or Right(true) if there is sufficient input and the stream is at EOF or Right(false) if there is simply not sufficient data to get the next entry and more needs to be read.
   */
  private[this] final def nextEntry(buffer: ByteBuffer): Either[TarArchiveEntry, Boolean] = {
    null
  }

  private[this] final def writingFile = null != fixedlengthconduit

  private[this] final var in: TarArchiveInputStream = null

  private[this] final var fixedlengthconduit: FixedLengthConduit = null

  private[this] final var recordsize = 0

  private[this] final var padsize = 0

}

/**
 *
 */
sealed trait TarArchiveConduitBase

  extends Conduit {

  def close = if (!isclosed) {
    println("close tar")
    isclosed = true
    releaseByteBuffer(entrybuffer)
    entrybuffer = null
  }

  def isOpen = !isclosed

  protected[this] final var entrybuffer: ByteBuffer = null

  protected[this] val directory: File

  protected[this] final val directorypath = createDirectories(directory.toPath.toAbsolutePath)

  private[this] final var isclosed = false

}
