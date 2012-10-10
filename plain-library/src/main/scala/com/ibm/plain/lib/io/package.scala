package com.ibm.plain

package lib

import java.io.{ File, IOException, InputStream, OutputStream, Reader, Writer }
import java.nio.ByteBuffer
import java.nio.channels.{ FileChannel, ReadableByteChannel, WritableByteChannel }
import java.nio.channels.Channels.newChannel
import java.nio.file.{ Files, Paths }
import java.util.concurrent.ConcurrentHashMap

import language.implicitConversions
import scala.collection.JavaConversions.collectionAsScalaIterable

import org.apache.commons.io.FileUtils

import lib.config.CheckedConfig

import config.config2RichConfig
import config.settings.{ getInt, getMilliseconds }

import logging.createLogger
import concurrent.{ spawn, sleep }

package object io

  extends CheckedConfig {

  import config._
  import config.settings._

  /**
   * Copies the entire inputstream to outputstream, then flushs the outputstream.
   */
  def copyBytes(in: InputStream, out: OutputStream, buffersize: Int = defaultBufferSize) {
    copyBytesNio(newChannel(in), newChannel(out), buffersize)
    out.flush
  }

  /**
   * Copies the entire inputstream to outputstream, then flushs the outputstream.
   */
  def copyBytesIo(in: InputStream, out: OutputStream, buffersize: Int = defaultBufferSize) {
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
  def copyBytesNio(in: ReadableByteChannel, out: WritableByteChannel, buffersize: Int = defaultBufferSize) {
    if (in.isInstanceOf[FileChannel]) {
      val f = in.asInstanceOf[FileChannel]
      f.transferTo(0, f.size, out)
    } else if (out.isInstanceOf[FileChannel]) {
      val f = out.asInstanceOf[FileChannel]
      f.transferFrom(in, 0, Long.MaxValue)
    } else {
      val b = ByteBuffer.allocateDirect(buffersize)
      var bytesread = 0
      while (-1 < { bytesread = in.read(b); bytesread }) {
        b.flip
        out.write(b)
        b.clear
      }
    }
  }

  /**
   * Copies the entire reader to writer, then flushs the writer.
   */
  def copyText(in: Reader, out: Writer, buffersize: Int = defaultBufferSize) {
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
  def copyLines(in: Reader, out: Writer, buffersize: Int = defaultBufferSize) {
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
  def copyFully(in: InputStream, buffersize: Int = defaultBufferSize) = {
    val out = new java.io.ByteArrayOutputStream
    copyBytes(in, out)
    out
  }

  /**
   * If not set differently this will result to 2k which proved to provide best performance under high load.
   */
  final val defaultBufferSize = getBytes("plain.io.default-buffersize", 2 * 1024).toInt

  /**
   * To make deleteDirectory more robust.
   */
  val deleteDirectoryRetries = getInt("plain.io.delete-directory-retries")

  val deleteDirectoryTimeout = getMilliseconds("plain.io.delete-directory-timeout")

  final val temp = try {
    val tmp = getString("plain.temp", System.getenv("TMP"))
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
  def temporaryFile = {
    val f = Files.createTempFile(temp, null, null).toFile
    deleteOnExit(f)
    f
  }

  /**
   * Create a temporary file in the given directory. It will be deleted at JVM shutdown.
   */
  def temporaryFileInDirectory(directory: File) = {
    val f = Files.createTempFile(directory.toPath, null, null).toFile
    deleteOnExit(f)
    f
  }

  /**
   * Create a temporary directory somewhere in the default location. It will be deleted at JVM shutdown together with all files it includes.
   */
  def temporaryDirectory = {
    val d = Files.createTempDirectory(temp, null).toFile
    deleteOnExit(d)
    d
  }

  /**
   * Delete a directory and all of its contents. Use delete-directory-retries and delete-directory-timeout to make this method more robust.
   */
  def deleteDirectory(directory: File) = spawn {
    try {
      if (directory.exists) FileUtils.deleteDirectory(directory)
    } catch {
      case e: IOException ⇒
        var retries = deleteDirectoryRetries
        while (0 < retries) {
          try {
            createLogger(this).info("deleteDirectory : retry " + retries + " " + directory)
            sleep(deleteDirectoryTimeout)
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
   * This file will be automatically deleted at JVM shutdown.
   */
  def deleteOnExit(file: File) = DeleteOnExit.add(file)

  private object DeleteOnExit {

    def add(file: File) = files.put(file.getAbsolutePath, file)

    def delete = {
      files.values.filter(!_.isDirectory).foreach(_.delete)
      files.values.filter(_.isDirectory).foreach { d ⇒ d.listFiles.foreach(_.delete); d.delete }
    }

    //  addShutdownHook(delete)

    private[this] val files = new ConcurrentHashMap[String, File]

    protected override def finalize = delete

  }

  /**
   * check requirements
   */
  require(null != temp, "Neither plain.temp config setting nor TMP environment variable nor java.io.tmpdir property are set.")

}
