package com.ibm

package plain

package aio

import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.{ DeflaterOutputStream ⇒ JDeflaterOutputStream, GZIPOutputStream ⇒ JGZIPOutputStream, Deflater, CRC32 }

/**
 *
 */
final class DeflaterOutputStream(

  output: OutputStream,

  compressionlevel: Int)

  extends JDeflaterOutputStream(output) {

  `def`.setLevel(compressionlevel)

  def this(output: OutputStream) = this(output, Deflater.BEST_SPEED)

}

/**
 * With this class it's possible to set the compression level on the GZIPOutputStream.
 */
final class GZIPOutputStream(

  output: OutputStream,

  buffersize: Int,

  syncmode: Boolean,

  compressionlevel: Int)

  extends JGZIPOutputStream(output, buffersize, syncmode) {

  `def`.setLevel(compressionlevel)

  def this(output: OutputStream) = this(output, defaultBufferSize, false, Deflater.BEST_SPEED)

  def this(output: OutputStream, compressionlevel: Int) = this(output, defaultBufferSize, false, compressionlevel)

  def this(output: OutputStream, buffersize: Int, syncmode: Boolean) = this(output, buffersize, syncmode, Deflater.DEFAULT_COMPRESSION)

}

/**
 *
 */
trait Compressor {

  def finish(buffer: ByteBuffer): ByteBuffer

  def compress(buffer: ByteBuffer): ByteBuffer

  def uncompress(buffer: ByteBuffer): ByteBuffer

}

/**
 * Prefer "deflate" over "gzip", most of the time it is at least 100% faster.
 */
final class GZIPCompressor private (

  compressionlevel: Int)

  extends Compressor {

  final def compress(buffer: ByteBuffer): ByteBuffer = {
    val length = buffer.remaining
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](length); buffer.get(a); buffer.clear; a }
    crc.update(array, 0, length)
    deflater.setInput(array, 0, length)
    val written = if (header) {
      header = false
      val w = deflater.deflate(array, 10, length - 10, Deflater.SYNC_FLUSH)
      Array.copy(GZIPCompressor.header, 0, array, 0, 10)
      10 + w
    } else {
      deflater.deflate(array, 0, length, Deflater.SYNC_FLUSH)
    }
    if (buffer.hasArray) buffer.position(written) else buffer.put(array, 0, written)
    buffer
  }

  final def finish(buffer: ByteBuffer): ByteBuffer = {
    deflater.finish
    val a = new Array[Byte](16)
    val written = deflater.deflate(a, 0, a.length, Deflater.FULL_FLUSH)
    buffer.put(a, 0, written)
    write(buffer, (crc.getValue % Int.MaxValue).toInt)
    write(buffer, (deflater.getBytesRead % Int.MaxValue).toInt)
    deflater.end
    buffer.flip
    buffer
  }

  final def uncompress(buffer: ByteBuffer): ByteBuffer = throw new UnsupportedOperationException

  @inline private[this] final def write(buffer: ByteBuffer, i: Int) = {
    buffer.put((i & 0xff).toByte)
    buffer.put(((i >> 8) & 0xff).toByte)
    buffer.put(((i >> 16) & 0xff).toByte)
    buffer.put(((i >> 24) & 0xff).toByte)
  }

  private[this] final val deflater = new Deflater(compressionlevel, true)

  private[this] final val crc = new CRC32

  private[this] final var header = true

}

object GZIPCompressor {

  def apply(compressionlevel: Int) = new GZIPCompressor(compressionlevel)

  private val header = Array[Byte](0x1f, 0x8b.toByte, Deflater.DEFLATED, 0, 0, 0, 0, 0, 0, 0)

}

/**
 * Prefer "deflate" over "gzip", most of the time it is at least 100% faster.
 */
final class DeflateCompressor private (

  compressionlevel: Int)

  extends Compressor {

  final def compress(buffer: ByteBuffer): ByteBuffer = {
    val length = buffer.remaining
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](length); buffer.get(a); buffer.clear; a }
    val (len, w) = if (header) {
      header = false
      val a = new Array[Byte](16)
      val len = scala.math.min(length, 16)
      Array.copy(array, 0, a, 0, len)
      deflater.setInput(a, 0, len)
      (len, deflater.deflate(array, 0, length, Deflater.NO_FLUSH))
    } else (0, 0)
    deflater.setInput(array, len, length - len)
    val written = deflater.deflate(array, w, length - w, Deflater.SYNC_FLUSH)
    if (buffer.hasArray) buffer.position(written + w) else buffer.put(array, 0, written + w)
    buffer
  }

  final def finish(buffer: ByteBuffer): ByteBuffer = {
    deflater.finish
    val a = new Array[Byte](16)
    val written = deflater.deflate(a, 0, a.length, Deflater.FULL_FLUSH)
    deflater.end
    buffer.put(a, 0, written)
    buffer.flip
    buffer
  }

  final def uncompress(buffer: ByteBuffer): ByteBuffer = throw new UnsupportedOperationException

  override final def toString = try { "DeflaterCompression(read: " + deflater.getBytesRead + ", written: " + deflater.getBytesWritten + ")" } catch { case _: Throwable ⇒ "DeflaterCompression(closed)" }

  private[this] final val deflater = new Deflater(compressionlevel, false)

  private[this] final var header = true

}

object DeflateCompressor {

  def apply(compressionlevel: Int) = new DeflateCompressor(compressionlevel)

}
