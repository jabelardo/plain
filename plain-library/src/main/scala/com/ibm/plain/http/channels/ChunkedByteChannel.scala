package com.ibm

package plain

package http

package channels

import java.io.StreamCorruptedException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

import scala.math.min

import aio.Integer
import aio.conduits.FilterByteChannel

/**
 *
 */
final class ChunkedByteChannel private (

  underlyingchannel: Channel)

  extends FilterByteChannel(

    underlyingchannel) {

  import ChunkedByteChannel._

  protected[this] final def filter(processed: Integer, buffer: ByteBuffer): Integer = {
    if (null == chunk) {
      chunk = nextChunk
    } else {
      chunk.setBuffer(conduitbuffer)
    }
    val len = chunk.consume(buffer)
    if (chunk.drained) chunk = null
    len
  }

  protected[this] final def hasSufficient = {
    if (7 > available) false else if (7 == available) finalchunk == new String(conduitbuffer.array, conduitbuffer.position, 7) else 12 <= available
  }

  private[this] final def nextChunk: Chunk = {
    val array = conduitbuffer.array
    val offset = conduitbuffer.position
    "(\r\n)*[0-9a-fA-F]+\r\n".r.findFirstIn(new String(array, offset, 12)) match {
      case Some(header) ⇒
        val len = Integer.parseInt(header.trim, 16)
        val avail = min(len, conduitbuffer.remaining - header.length)
        val chunk = Chunk(len, ByteBuffer.wrap(array, offset + header.length, avail), 0)
        conduitbuffer.position(chunk.buffer.limit)
        chunk
      case _ ⇒ throw new StreamCorruptedException("Invalid chunk header")
    }
  }

  private[this] final case class Chunk(

    length: Int,

    var buffer: ByteBuffer,

    var position: Int) {

    @inline final def setBuffer(inner: ByteBuffer) = {
      buffer = ByteBuffer.wrap(inner.array, inner.position, length - position)
      conduitbuffer.position(buffer.limit)
    }

    @inline final def consume(outer: ByteBuffer): Int = {
      val e = outer.remaining
      outer.put(buffer)
      val len = e - outer.remaining
      outer.flip
      position += len
      len
    }

    @inline final def drained = length == position

  }

  private[this] final var chunk: Chunk = null

}

/**
 *
 */
object ChunkedByteChannel {

  final def apply(underlyingchannel: Channel) = new ChunkedByteChannel(underlyingchannel)

  private final val finalchunk = "\r\n0\r\n\r\n"

}