package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.{ BufferOverflowException, ByteBuffer }
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

import scala.math.min

/**
 * A FilterConduit modifies or manipulates the data of its underlying channel during reads and writes.
 */
sealed trait FilterBaseConduit[C <: Channel]

  extends Conduit[C] {

  protected[this] final def skip(n: Int): Int = {
    val e = innerbuffer.position
    val skip = min(n, innerbuffer.remaining)
    innerbuffer.position(innerbuffer.position + skip)
    skip
  }

  protected[this] val innerbuffer = { val b = ByteBuffer.wrap(new Array[Byte](defaultBufferSize)); b.flip; b }

}

/**
 *
 */
trait FilterSourceConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with SourceConduit[C] {

  protected[this] def filter(processed: Integer, buffer: ByteBuffer): Integer

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isDrained) {
      innerbuffer.clear
      underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
    } else {
      if (checkSufficient) {
        handler.completed(filter(innerbuffer.remaining, buffer), attachment)
      } else {
        handleOverflow
        underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
      }
    }

  }

  protected[this] def hasSufficient: Boolean

  protected[this] final def isDrained = 0 == innerbuffer.remaining

  protected[this] final def available = innerbuffer.remaining

  private[this] final def handleOverflow = {
    val overflow = ByteBuffer.wrap(innerbuffer.array, 0, innerbuffer.position)
    require(overflow.remaining >= innerbuffer.remaining, throw new BufferOverflowException)
    overflow.put(innerbuffer)
    overflow.flip
    innerbuffer.position(overflow.limit)
    innerbuffer.limit(innerbuffer.capacity)
  }

  private[this] final def checkSufficient = !isDrained && hasSufficient

  private[this] final class FilterSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    final def completed(processed: Integer, attachment: A) = {
      innerbuffer.flip
      handler.completed(filter(processed, buffer), attachment)
    }

  }

}

/**
 *
 */
trait FilterSinkConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with SinkConduit[C] {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {}

}

