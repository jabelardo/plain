package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.zip.Inflater

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

  extends FilterSourceConduit {

  protected[this] def filter(processed: Integer, buffer: ByteBuffer): Integer = {
    if (inflater.finished) {
      0
    } else {
      if (inflater.needsInput) {
        inflater.setInput(innerbuffer.array, innerbuffer.position, innerbuffer.remaining)
      }
      val (array, offset, length) = if (buffer.hasArray) {
        (buffer.array, buffer.position, buffer.remaining)
      } else {
        if (null == inflatearray) inflatearray = new Array[Byte](buffer.capacity)
        (inflatearray, 0, buffer.remaining)
      }
      val e = inflater.getRemaining
      try {
        val len = inflater.inflate(array, offset, length)
        skip(e - inflater.getRemaining)
        if (!buffer.hasArray) buffer.put(array, 0, len)
        len
      } catch {
        case e: Throwable ⇒
          println("inner " + format(innerbuffer))
          println("outer " + format(buffer))
          println(inflater.getBytesRead + " " + inflater.getBytesWritten + " " + inflater.getRemaining)
          throw e
      }
    }
  }

  protected[this] def finish(buffer: ByteBuffer) = inflater.end

  protected[this] def hasSufficient = 0 < inflater.getRemaining

  protected[this] val nowrap: Boolean

  protected[this] final val inflater = new Inflater(nowrap)

  protected[this] final var inflatearray: Array[Byte] = null

}

/**
 * Sink conduit.
 */
trait DeflateSinkConduit

  extends FilterSinkConduit {

}

