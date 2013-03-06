package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.{ Deflater, CRC32 }

import aio.Encoder
/**
 * Prefer "deflate" over "gzip", most of the time it is at least 100% faster.
 */
final class DeflateEncoder private (

  compressionlevel: Int)

  extends Encoder {

  final def name = "deflate"

  final def encode(buffer: ByteBuffer) = {
    val length = buffer.remaining
    val chunklen = 12
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](length); buffer.get(a); buffer.clear; a }
    val (len, w) = if (header) {
      header = false
      val len = chunklen + 16
      val a = new Array[Byte](len)
      Array.copy(array, 0, a, 0, len)
      deflater.setInput(a, 0, len)
      (len, deflater.deflate(array, chunklen, length - chunklen, Deflater.NO_FLUSH))
    } else (0, 0)
    deflater.setInput(array, len, length - len)
    val written = deflater.deflate(array, chunklen + w, length - (chunklen + w), Deflater.SYNC_FLUSH)
    Array.copy("\r\n%08x\r\n".format(written + w).getBytes, 0, array, 0, chunklen)
    if (buffer.hasArray) buffer.position(12 + written + w) else buffer.put(array, 0, 12 + written + w)
  }

  final def finish(buffer: ByteBuffer) = {
    deflater.finish
    val a = new Array[Byte](16)
    val written = deflater.deflate(a, 0, a.length, Deflater.FULL_FLUSH)
    deflater.end
    buffer.put("\r\n%x\r\n".format(written).getBytes)
    buffer.put(a, 0, written)
    buffer.put("\r\n0\r\n\r\n".getBytes)
    buffer.flip
  }

  override final def toString = try { "DeflaterCompression(read: " + deflater.getBytesRead + ", written: " + deflater.getBytesWritten + ")" } catch { case _: Throwable ⇒ "DeflaterCompression(closed)" }

  private[this] final val deflater = new Deflater(compressionlevel, false)

  private[this] final var header = true

}

object DeflateEncoder {

  def apply(compressionlevel: Int) = new DeflateEncoder(compressionlevel)

}

/**
 * Prefer "deflate" over "gzip", most of the time it is at least 100% faster.
 */
final class GZIPEncoder private (

  compressionlevel: Int)

  extends Encoder {

  final def name = "gzip"

  final def encode(buffer: ByteBuffer) = {
    val length = buffer.remaining
    val chunklen = 12
    val headerlen = 10
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](length); buffer.get(a); buffer.clear; a }
    crc.update(array, 0, length)
    deflater.setInput(array, 0, length)
    val written = if (header) {
      header = false
      val l = chunklen + headerlen
      val w = deflater.deflate(array, chunklen + headerlen, length - (chunklen + headerlen), Deflater.SYNC_FLUSH)
      Array.copy(GZIPEncoder.header, 0, array, chunklen, headerlen)
      headerlen + w
    } else {
      deflater.deflate(array, chunklen, length - chunklen, Deflater.SYNC_FLUSH)
    }
    Array.copy("\r\n%08x\r\n".format(written).getBytes, 0, array, 0, chunklen)
    if (buffer.hasArray) buffer.position(written + chunklen) else buffer.put(array, 0, written + chunklen)
  }

  final def finish(buffer: ByteBuffer) = {
    deflater.finish
    val a = new Array[Byte](16)
    val written = deflater.deflate(a, 0, a.length, Deflater.FULL_FLUSH)
    buffer.put("\r\n%x\r\n".format(written + 8).getBytes)
    buffer.put(a, 0, written)
    write(buffer, crc.getValue)
    write(buffer, deflater.getBytesRead)
    buffer.put("\r\n0\r\n\r\n".getBytes)
    deflater.end
    buffer.flip
  }

  override final def toString = try { "GZIPCompression(read: " + deflater.getBytesRead + ", written: " + (10 + deflater.getBytesWritten + 8) + ")" } catch { case _: Throwable ⇒ "GZIPCompression(closed)" }

  @inline private[this] final def write(buffer: ByteBuffer, l: Long) = {
    val v = (l % 4294967296L).toInt
    buffer.put((v & 0xff).toByte)
    buffer.put(((v >> 8) & 0xff).toByte)
    buffer.put(((v >> 16) & 0xff).toByte)
    buffer.put(((v >> 24) & 0xff).toByte)
  }

  private[this] final val deflater = new Deflater(compressionlevel, true)

  private[this] final val crc = new CRC32

  private[this] final var header = true

}

object GZIPEncoder {

  def apply(compressionlevel: Int) = new GZIPEncoder(compressionlevel)

  private val header = Array[Byte](0x1f, 0x8b.toByte, Deflater.DEFLATED, 0, 0, 0, 0, 0, 4, 0xff.toByte)

}
