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
    println("deflate " + processed)
    if (0 >= processed) {
      processed
    } else {
      println(format(buffer))
      println(format(innerbuffer))
      if (deflater.needsInput) {
        deflater.setInput(buffer.array, buffer.position, buffer.remaining)
      }
      val len = deflater.deflate(innerbuffer.array, innerbuffer.position, innerbuffer.remaining)
      println(len)
      println(innerbuffer)
      len
    }
  }

  protected[this] val nowrap: Boolean

  protected[this] final val deflater = new Deflater(Deflater.BEST_SPEED, nowrap)

}

