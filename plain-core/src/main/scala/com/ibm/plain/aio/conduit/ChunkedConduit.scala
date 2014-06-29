package com.ibm

package plain

package aio

package conduit

import java.io.StreamCorruptedException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.Arrays

import scala.math.min

/**
 *
 */
final class ChunkedConduit private (

  protected[this] final val underlyingchannel: Channel)

    extends ChunkedSourceConduit

    with ChunkedSinkConduit {

}

/**
 *
 */
object ChunkedConduit {

  final def apply(underlyingchannel: Channel) = new ChunkedConduit(underlyingchannel)

}

/**
 * Source conduit.
 */
sealed trait ChunkedSourceConduit

    extends ChunkedConduitBase

    with FilterSourceConduit[Channel] {

  protected[this] final def filterIn(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      processed
    } else {
      if (null == chunk) {
        chunk = nextChunk
      } else {
        chunk.fill(innerbuffer)
      }
      chunk.drain(buffer)
    }
  }

  protected[this] final def hasSufficient = {
    if (finalheadersize > available)
      false
    else if (finalheadersize == available)
      finalheader == new String(innerbuffer.array, innerbuffer.position, finalheadersize)
    else
      maxheadersize <= available
  }

  private[this] final def nextChunk: Chunk = {
    val array = innerbuffer.array
    val offset = innerbuffer.position
    "(\r\n)*[0-9a-fA-F]+\r\n".r.findFirstIn(new String(array, offset, min(maxheadersize, innerbuffer.remaining))) match {
      case Some(header) ⇒
        val headerlen = header.length
        val hexradix = 16
        val chunklen = Integer.parseInt(header.trim, hexradix)
        val avail = min(chunklen, innerbuffer.remaining - headerlen)
        val chunk = new Chunk(ByteBuffer.wrap(array, offset + headerlen, avail), 0, chunklen)
        innerbuffer.position(chunk.buffer.limit)
        if (0 == chunklen) skip(2)
        chunk
      case _ ⇒
        throw new StreamCorruptedException("Invalid chunk header : " + (new String(array, offset, min(maxheadersize, innerbuffer.remaining)).getBytes.toList))
    }
  }

}

/**
 * Sink conduit.
 */
sealed trait ChunkedSinkConduit

    extends ChunkedConduitBase

    with FilterSinkConduit[Channel] {

  protected[this] final def filterOut(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      lastChunk
      processed
    } else {
      if (null == chunk) {
        chunk = nextChunk(buffer)
      } else {
        chunk.fill(buffer)
      }
      chunk.drain(innerbuffer)
    }
  }

  private[this] final def nextChunk(buffer: ByteBuffer): Chunk = {
    val avail = min(innerbuffer.remaining - headersize, min(buffer.remaining, defaultChunkSize))
    val chunk = new Chunk(ByteBuffer.wrap(buffer.array, buffer.position, avail), 0, avail)
    buffer.position(buffer.position + avail)
    innerbuffer.put(header.format(chunk.length).getBytes)
    chunk
  }

  private[this] final def lastChunk = {
    require(finalheadersize <= innerbuffer.remaining, "Not enough space to write final chunk : " + innerbuffer.remaining)
    innerbuffer.put(finalheader.getBytes)
  }

}

/**
 * Some common basics.
 */
abstract sealed class ChunkedConduitBase {

  protected[this] final class Chunk(

      var buffer: ByteBuffer,

      var position: Int,

      val length: Int) {

    final def fill(source: ByteBuffer) = {
      buffer = ByteBuffer.wrap(source.array, source.position, min(length - position, source.remaining))
      source.position(min(buffer.limit, source.limit))
    }

    final def drain(sink: ByteBuffer): Int = {
      val e = sink.remaining
      sink.put(buffer.array, buffer.position, min(buffer.remaining, sink.remaining))
      val len = e - sink.remaining
      position += len
      if (length == position) chunk = null
      len
    }

  }

  protected[this] final var chunk: Chunk = null

  protected[this] final val header = "\r\n%04x\r\n"

  protected[this] final val finalheader = "\r\n0\r\n\r\n"

  protected[this] final val maxheadersize = 12

  protected[this] final val headersize = header.format(0).getBytes.size

  protected[this] final val finalheadersize = finalheader.getBytes.size

}
