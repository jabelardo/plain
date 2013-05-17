package com.ibm

package plain

import java.io.{ File, IOException, InputStream, OutputStream, Reader, Writer, BufferedOutputStream }
import java.nio.ByteBuffer
import java.nio.channels.{ FileChannel, ReadableByteChannel, WritableByteChannel }
import java.nio.channels.Channels.newChannel
import java.nio.file.{ Files, Paths }
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }
import org.apache.commons.io.FileUtils
import concurrent.{ sleep, spawn }
import config.config2RichConfig
import config.settings.{ getInt, getMilliseconds }
import config.CheckedConfig
import io.Io
import logging.createLogger

package object io

  extends CheckedConfig {

  import config._
  import config.settings._

  /**
   * Copies the entire inputstream to outputstream, then flushs the outputstream.
   */
  final def copyBytes(in: InputStream, out: OutputStream, buffersize: Int = defaultBufferSize) {
    copyBytesNio(newChannel(in), newChannel(out), buffersize)
    out.flush
  }

  /**
   * Copies the entire inputstream to outputstream, then flushs the outputstream.
   */
  final def copyBytesIo(in: InputStream, out: OutputStream, buffersize: Int = defaultBufferSize) {
    val buffer = new Array[Byte](buffersize)
    var bytesread = 0
    while (-1 < { bytesread = in.read(buffer, 0, buffersize); bytesread }) {
      out.write(buffer, 0, bytesread)
    }
    out.flush
  }

  /**
   * Copies the entire readable channel to the writable channel.
   */
  final def copyBytesNio(in: ReadableByteChannel, out: WritableByteChannel, buffersize: Int = defaultBufferSize) {
    if (in.isInstanceOf[FileChannel]) {
      val f = in.asInstanceOf[FileChannel]
      f.transferTo(0, f.size, out)
    } else if (out.isInstanceOf[FileChannel]) {
      val f = out.asInstanceOf[FileChannel]
      f.transferFrom(in, 0, Long.MaxValue)
    } else {
      val b = aio.bestFitByteBuffer(buffersize)
      try {
        var bytesread = 0
        while (-1 < { bytesread = in.read(b); bytesread }) {
          b.flip
          out.write(b)
          b.clear
        }
      } finally aio.releaseByteBuffer(b)
    }
  }

  /**
   * Copies the entire reader to writer, then flushs the writer.
   */
  final def copyText(in: Reader, out: Writer, buffersize: Int = defaultBufferSize) {
    var bytesread = 0
    val buffer = new Array[Char](buffersize)
    while (-1 < { bytesread = in.read(buffer, 0, buffersize); bytesread }) {
      out.write(buffer, 0, bytesread)
    }
    out.flush
  }

  /**
   * Copies the entire reader to writer line by line to fix newline problems, then flushs the writer.
   */
  final def copyLines(in: Reader, out: Writer, buffersize: Int = defaultBufferSize) {
    val reader = new java.io.LineNumberReader(in)
    val writer = new java.io.PrintWriter(out)
    var line: String = null
    while (null != { line = reader.readLine; line }) {
      writer.println(line)
    }
    writer.flush
  }

  /**
   * Copies the entire inputstream to a ByteArrayOutputStream
   */
  final def copyFully(in: InputStream, buffersize: Int = defaultBufferSize) = {
    val out = new java.io.ByteArrayOutputStream
    copyBytes(in, out)
    out
  }

  /**
   * Compress a ByteBuffer in place using GZIP.
   */
  final def gzip(buffer: ByteBuffer): ByteBuffer = {
    val in = new ByteBufferInputStream(buffer)
    val out = new GZIPOutputStream(new BufferedOutputStream(new ByteBufferOutputStream(buffer)), defaultBufferSize)
    copyBytesIo(in, out)
    out.close
    buffer
  }

  /**
   * Decompress a ByteBuffer in place using GZIP. It assumes that the gunzipped content will fit into its capacity.
   */
  final def gunzip(buffer: ByteBuffer): ByteBuffer = {
    val in = new GZIPInputStream(new ByteBufferInputStream(buffer))
    val out = new BufferedOutputStream(new ByteBufferOutputStream(buffer))
    copyBytesIo(in, out)
    out.close
    buffer
  }

  /**
   * If not set differently this will result to 2k which proved to provide best performance under high load.
   */
  final val defaultBufferSize = getBytes("plain.io.default-buffer-size", 2 * 1024).toInt

  /**
   * To make deleteDirectory more robust.
   */
  final val deleteDirectoryRetries = getInt("plain.io.delete-directory-retries", 5)

  final val deleteDirectoryPauseBetweenRetries = getMilliseconds("plain.io.delete-directory-pause-between-retries", 10000)

  final val temp = try {
    val tmp = getString("plain.config.temp", System.getenv("TMP"))
    Files.createDirectories(Paths.get(tmp))
    System.setProperty("java.io.tmpdir", tmp)
    Paths.get(tmp)
  } catch {
    case _: Throwable ⇒
      Paths.get(System.getProperty("java.io.tmpdir"))
  }

  /**
   * Create a temporary file somewhere in the default location. It will be deleted at JVM shutdown.
   */
  final def temporaryFile = {
    val f = Files.createTempFile(temp, null, null).toFile
    deleteOnExit(f)
    f
  }

  /**
   * Create a temporary file in the given directory. It will be deleted at JVM shutdown.
   */
  final def temporaryFileInDirectory(directory: File) = {
    val f = Files.createTempFile(directory.toPath, null, null).toFile
    deleteOnExit(f)
    f
  }

  /**
   * Create a temporary directory somewhere in the default location. It will be deleted at JVM shutdown together with all files it includes.
   */
  final def temporaryDirectory = {
    val d = Files.createTempDirectory(temp, null).toFile
    deleteOnExit(d)
    d
  }

  /**
   * Delete a directory and all of its contents in a background thread. Use delete-directory-retries and delete-directory-timeout to make this method more robust.
   */
  final def deleteDirectory(directory: File) = spawn {
    try {
      if (directory.exists) FileUtils.deleteDirectory(directory)
    } catch {
      case e: IOException ⇒
        var retries = deleteDirectoryRetries
        while (0 < retries) {
          try {
            createLogger(this).info("deleteDirectory : retry " + retries + " " + directory)
            sleep(deleteDirectoryPauseBetweenRetries)
            FileUtils.deleteDirectory(directory)
            retries = 0
          } catch {
            case e: IOException ⇒ retries -= 1
          }
        }
      case e: Throwable ⇒ createLogger(this).debug("Could not delete directory : " + e)
    }
  }

  /**
   * The file given will be automatically deleted at JVM shutdown.
   */
  final def deleteOnExit(file: File) = Io.add(file)

  /**
   * This can be accessed as a static field (eg. for Derby database, as a null logger).
   */
  final val devnull = NullOutputStream

  /**
   * check requirements
   */
  require(null != temp, "Neither plain.temp config setting nor TMP environment variable nor java.io.tmpdir property are set.")

}
