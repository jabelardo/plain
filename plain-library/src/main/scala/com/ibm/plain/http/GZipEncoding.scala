package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.{ Deflater, CRC32 }

import aio.Encoder

/**
 * Prefer "deflate" over "gzip".
 */
final class GzipEncoder private (

  compressionlevel: Int)

  extends BaseEncoder(compressionlevel, true) {

  final def name = "gzip"

  final def encode(buffer: ByteBuffer) = {
    if (null == array) array = new Array[Byte](buffer.capacity)
    val offset = buffer.position
    val length = buffer.remaining
    val chunklen = 12
    val headerlen = 10
    buffer.get(array, offset, length)
    buffer.position(offset)
    crc.update(array, offset, length)
    deflater.setInput(array, offset, length)
    val written = if (header) {
      header = false
      val w = deflater.deflate(array, offset + chunklen + headerlen, length - (chunklen + headerlen), Deflater.SYNC_FLUSH)
      Array.copy(GzipEncoder.header, 0, array, offset + chunklen, headerlen)
      headerlen + w
    } else {
      deflater.deflate(array, offset + chunklen, array.length - (offset + chunklen), Deflater.SYNC_FLUSH)
    }
    require(written < array.length - (offset + chunklen), "plain.aio.encoding-spare-buffer-space is too small, encoded data larger than un-encoded.")
    Array.copy(chunk(written), 0, array, offset, chunklen)
    if (chunklen + written > buffer.remaining) buffer.limit(buffer.position + chunklen + written)
    buffer.put(array, offset, chunklen + written)
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

object GzipEncoder {

  def apply(compressionlevel: Int) = new GzipEncoder(compressionlevel)

  private final val header = Array[Byte](0x1f, 0x8b.toByte, Deflater.DEFLATED, 0, 0, 0, 0, 0, 4, 0xff.toByte)

}
