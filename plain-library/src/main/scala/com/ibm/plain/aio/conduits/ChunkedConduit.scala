package com.ibm

package plain

package aio

package conduits

import java.io.StreamCorruptedException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.Arrays

import scala.math.min

/**
 *
 */
final class ChunkedConduit private (

  protected[this] val underlyingchannel: Channel)

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
    if (7 > available) false else if (7 == available) finalchunk == new String(innerbuffer.array, innerbuffer.position, 7) else 12 <= available
  }

  private[this] final def nextChunk: Chunk = {
    val array = innerbuffer.array
    val offset = innerbuffer.position
    "(\r\n)*[0-9a-fA-F]+\r\n".r.findFirstIn(new String(array, offset, 12)) match {
      case Some(header) ⇒
        val headerlen = header.length
        val chunklen = Integer.parseInt(header.trim, 16)
        val avail = min(chunklen, innerbuffer.remaining - headerlen)
        val chunk = new Chunk(ByteBuffer.wrap(array, offset + headerlen, avail), 0, chunklen)
        innerbuffer.position(chunk.chunkbuffer.limit)
        if (0 == chunklen) skip(2)
        chunk
      case _ ⇒ throw new StreamCorruptedException("Invalid chunk header : " + (new String(array, offset, 12).getBytes.toList))
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
      processed
    } else {
      if (null == chunk) {
        chunk = new Chunk(buffer, 0, buffer.remaining)
        innerbuffer.clear
        innerbuffer.put("\r\n%x\r\n".format(buffer.remaining).getBytes)
      }
      chunk.drain(innerbuffer)
      innerbuffer.flip
      println(format(buffer))
      println(format(innerbuffer))
      buffer.remaining
    }
  }

}

/**
 * Some common basics.
 */
abstract sealed class ChunkedConduitBase {

  protected[this] final class Chunk(

    var chunkbuffer: ByteBuffer,

    var position: Int,

    val length: Int) {

    final def fill(source: ByteBuffer) = {
      chunkbuffer = ByteBuffer.wrap(source.array, source.position, length - position)
      source.position(min(chunkbuffer.limit, source.limit))
    }

    final def drain(sink: ByteBuffer): Int = {
      val e = sink.remaining
      sink.put(chunkbuffer)
      val len = e - sink.remaining
      position += len
      if (length == position) chunk = null
      len
    }

  }

  protected[this] final var chunk: Chunk = null

  protected[this] final val finalchunk = "\r\n0\r\n\r\n"

}
