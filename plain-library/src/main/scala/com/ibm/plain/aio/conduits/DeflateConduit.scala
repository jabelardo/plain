package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.zip.{ Deflater, Inflater }

/**
 *
 */
final class DeflateConduit private (

  protected[this] val underlyingchannel: Channel)

  extends DeflateSourceConduit

  with DeflateSinkConduit {

  protected[this] final val nowrap = false

}

/**
 *
 */
object DeflateConduit {

  final def apply(underlyingchannel: Channel) = new DeflateConduit(underlyingchannel)

}

/**
 * Source conduit.
 */
trait DeflateSourceConduit

  extends FilterSourceConduit[Channel] {

  protected[this] def filterIn(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      inflater.end
      processed
    } else {
      if (inflater.needsInput) {
        inflater.setInput(innerbuffer.array, innerbuffer.position, innerbuffer.remaining)
      }
      val e = inflater.getRemaining
      val len = inflater.inflate(buffer.array, buffer.position, buffer.remaining)
      skip(e - inflater.getRemaining)
      buffer.limit(buffer.position + len)
      len
    }
  }

  protected[this] def hasSufficient = 0 < inflater.getRemaining

  protected[this] val nowrap: Boolean

  protected[this] final val inflater = new Inflater(nowrap)

}

/**
 * Sink conduit.
 */
trait DeflateSinkConduit

  extends FilterSinkConduit[Channel] {

  protected[this] def filterOut(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      deflater.finish
      val len = deflater.deflate(innerbuffer.array, innerbuffer.position, innerbuffer.remaining, Deflater.FULL_FLUSH)
      deflater.end
      innerbuffer.position(innerbuffer.position + len)
      processed
    } else {
      if (deflater.needsInput) deflater.setInput(buffer.array, buffer.position, buffer.remaining)
      val len = deflater.deflate(innerbuffer.array, innerbuffer.position, innerbuffer.remaining, Deflater.SYNC_FLUSH)
      innerbuffer.position(innerbuffer.position + len)
      val delta = (deflater.getBytesRead - bytesread).toInt
      bytesread = deflater.getBytesRead
      buffer.position(buffer.position + delta)
      delta
    }
  }

  protected[this] val nowrap: Boolean

  protected[this] final val deflater = new Deflater(Deflater.BEST_SPEED, nowrap)

  private[this] final var bytesread = 0L

}

