package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.{ Deflater, CRC32 }

import aio.Encoder

abstract sealed class BaseEncoder(

  compressionlevel: Int,

  nowrap: Boolean)

  extends Encoder {

  final val text = name.getBytes

  final def finish(buffer: ByteBuffer) = {
    deflater.finish
    val a = new Array[Byte](16)
    val written = deflater.deflate(a, 0, a.length, Deflater.FULL_FLUSH)
    buffer.put("\r\n%x\r\n".format(written + footerlength).getBytes)
    buffer.put(a, 0, written)
    footer(buffer)
    buffer.put("\r\n0\r\n\r\n".getBytes)
    buffer.flip
    deflater.end
  }

  final override def toString = {
    val simplename = getClass.getSimpleName
    try { simplename + "(read: " + bytesRead + ", written: " + bytesWritten + ")" } catch { case _: Throwable ⇒ simplename + "(already closed)" }
  }

  protected[this] final val deflater = new Deflater(compressionlevel, nowrap)

  protected[this] final var header = true

  protected[this] def footer(buffer: ByteBuffer)

  protected[this] val footerlength: Int

  @inline protected[this] final def chunk(length: Int) = "\r\n%08x\r\n".format(length).getBytes

  protected[this] final def bytesRead = deflater.getBytesRead

  protected[this] def bytesWritten: Long

}

/**
 * Prefer "deflate" over "gzip", depending on input it can be more than 100% faster.
 */
final class DeflateEncoder private (

  compressionlevel: Int)

  extends BaseEncoder(compressionlevel, false) {

  final def name = "deflate"

  final def encode(buffer: ByteBuffer) = {
    val offset = buffer.position
    val length = buffer.remaining
    val chunklen = 12
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](offset + length); buffer.get(a, offset, length); buffer.position(offset); a }
    val (len, w) = if (header) {
      header = false
      val len = chunklen + 16
      val a = new Array[Byte](len)
      Array.copy(array, offset, a, 0, len)
      deflater.setInput(a, 0, len)
      (len, deflater.deflate(array, offset + chunklen, length - chunklen, Deflater.NO_FLUSH))
    } else (0, 0)
    deflater.setInput(array, offset + len, length - len)
    val written = deflater.deflate(array, offset + chunklen + w, length - (chunklen + w), Deflater.SYNC_FLUSH)
    Array.copy(chunk(written + w), 0, array, offset, chunklen)
    if (buffer.hasArray) buffer.position(offset + chunklen + written + w) else buffer.put(array, offset, chunklen + written + w)
  }

  protected[this] final def footer(buffer: ByteBuffer) = ()

  protected[this] final val footerlength = 0

  protected[this] final def bytesWritten = deflater.getBytesWritten

}

object DeflateEncoder {

  def apply(compressionlevel: Int) = new DeflateEncoder(compressionlevel)

}

/**
 * Prefer "deflate" over "gzip".
 */
final class GZIPEncoder private (

  compressionlevel: Int)

  extends BaseEncoder(compressionlevel, true) {

  final def name = "gzip"

  final def encode(buffer: ByteBuffer) = {
    val offset = buffer.position
    val length = buffer.remaining
    val chunklen = 12
    val headerlen = 10
    val array = if (buffer.hasArray) buffer.array else { val a = new Array[Byte](offset + length); buffer.get(a, offset, length); buffer.position(offset); a }
    crc.update(array, offset, length)
    deflater.setInput(array, offset, length)
    val written = if (header) {
      header = false
      val w = deflater.deflate(array, offset + chunklen + headerlen, length - (chunklen + headerlen), Deflater.SYNC_FLUSH)
      Array.copy(GZIPEncoder.header, 0, array, offset + chunklen, headerlen)
      headerlen + w
    } else {
      deflater.deflate(array, offset + chunklen, length - chunklen, Deflater.SYNC_FLUSH)
    }
    Array.copy(chunk(written), 0, array, offset, chunklen)
    if (buffer.hasArray) buffer.position(offset + chunklen + written) else buffer.put(array, offset, chunklen + written)
  }

  protected[this] final def footer(buffer: ByteBuffer) = {
    @inline def write(buffer: ByteBuffer, l: Long) = {
      val v = (l % 4294967296L)
      buffer.put((v & 0xff).toByte)
      buffer.put(((v >> 8) & 0xff).toByte)
      buffer.put(((v >> 16) & 0xff).toByte)
      buffer.put(((v >> 24) & 0xff).toByte)
    }
    write(buffer, crc.getValue)
    write(buffer, deflater.getBytesRead)
  }

  protected[this] final val footerlength = 8

  protected[this] final def bytesWritten = 10 + deflater.getBytesWritten + 8

  private[this] final val crc = new CRC32

}

object GZIPEncoder {

  def apply(compressionlevel: Int) = new GZIPEncoder(compressionlevel)

  private val header = Array[Byte](0x1f, 0x8b.toByte, Deflater.DEFLATED, 0, 0, 0, 0, 0, 4, 0xff.toByte)

}
