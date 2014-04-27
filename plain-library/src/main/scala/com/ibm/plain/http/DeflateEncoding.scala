package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.Deflater

import aio.Encoder

/**
 *
 */
abstract class BaseEncoder(

  compressionlevel: Int,

  nowrap: Boolean)

  extends Encoder {

  final val text = name.getBytes

  final def finish(buffer: ByteBuffer) = {
    deflater.finish
    val a = new Array[Byte](16) // a placeholder
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

  protected[this] final var array: Array[Byte] = null

}

/**
 * Prefer "deflate" over "gzip", depending on input it can be more than 100% faster.
 */
final class DeflateEncoder private (

  compressionlevel: Int)

  extends BaseEncoder(compressionlevel, false) {

  final def name = "deflate"

  final def encode(buffer: ByteBuffer) = {
    if (null == array) array = new Array[Byte](buffer.capacity)
    val offset = buffer.position
    val length = buffer.remaining
    val chunklen = 12
    buffer.get(array, offset, length)
    buffer.position(offset)
    val (len, w) = if (header) {
      header = false
      val len = chunklen + 16
      Array.copy(array, offset, a, 0, len)
      deflater.setInput(a, 0, len)
      (len, deflater.deflate(array, offset + chunklen, length - chunklen, Deflater.NO_FLUSH))
    } else (0, 0)
    deflater.setInput(array, offset + len, length - len)
    val writelen = array.length - (offset + chunklen + w)
    val written = deflater.deflate(array, offset + chunklen + w, writelen, Deflater.SYNC_FLUSH)
    require(written < writelen, "plain.aio.encoding-spare-buffer-space is too small, encoded data larger than un-encoded.")
    Array.copy(chunk(written + w), 0, array, offset, chunklen)
    if (chunklen + written + w > buffer.remaining) buffer.limit(buffer.position + chunklen + written + w)
    buffer.put(array, offset, chunklen + written + w)
  }

  protected[this] final def footer(buffer: ByteBuffer) = ()

  protected[this] final val footerlength = 0

  protected[this] final def bytesWritten = deflater.getBytesWritten

  private[this] final val a = new Array[Byte](12 + 16)

}

object DeflateEncoder {

  def apply(compressionlevel: Int) = new DeflateEncoder(compressionlevel)

}

