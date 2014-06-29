package com.ibm

package plain

package aio

package conduit

import java.nio.{ BufferUnderflowException, ByteBuffer }
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.concurrent.atomic.AtomicBoolean

import scala.math.min

/**
 * A FilterConduit filters data from an outer buffer to an inner buffer and modifies it on the way. As source and sink share the same inner buffer either reads or writes ares supported, but not both at the same time.
 */
trait FilterConduit[C <: Channel]

  extends FilterSourceConduit[C]

  with FilterSinkConduit[C]

/**
 * A FilterSourceConduit modifies or manipulates the data of its underlying channel during reads.
 */
trait FilterSourceConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with ConnectorSourceConduit[C] {

  /**
   * Needs to be implemented by subclasses. It filters from the outer buffer to the inner buffer.
   *
   * @param processed The bytes avaiable or the space on the inner buffer to be filtered to.
   * @param buffer The outer buffer to be filtered from.
   * @return The amount of bytes read and filtered from the outer buffer. Invariant: returns a value in [0, buffer.remaining]
   */
  protected[this] def filterIn(processed: Integer, buffer: ByteBuffer): Integer

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (drained) {
      innerbuffer.clear
      underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
    } else {
      if (hasSufficient) {
        handler.completed(filterIn(innerbuffer.remaining, buffer), attachment)
      } else {
        underflow
        underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
      }
    }

  }

  protected[this] def hasSufficient: Boolean

  protected[this] final def available = innerbuffer.remaining

  private[this] final def drained = 0 == innerbuffer.remaining

  private[this] final def underflow = {
    val buffer = ByteBuffer.wrap(innerbuffer.array, 0, innerbuffer.position)
    require(buffer.remaining >= innerbuffer.remaining, throw new BufferUnderflowException)
    buffer.put(innerbuffer)
    buffer.flip
    innerbuffer.position(buffer.limit)
    innerbuffer.limit(innerbuffer.capacity)
  }

  private[this] final class FilterSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 < innerbuffer.position) innerbuffer.flip
      handler.completed(filterIn(processed, buffer), attachment)
    }

  }

}

/**
 * A FilterSinkConduit modifies or manipulates the data of its underlying channel during writes.
 */
trait FilterSinkConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with ConnectorSinkConduit[C] {

  /**
   * Needs to be implemented by subclasses. It filters from the outer buffer to the inner buffer.
   *
   * @param processed The bytes processed on buffer. This amount of bytes can now be filtered.
   * @param buffer The outer buffer to be filtered.
   * @return The amount of bytes filtered on buffer. Invariant: returns a value in [0, processed]
   */
  protected[this] def filterOut(processed: Integer, buffer: ByteBuffer): Integer

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (filled) {
      spill(attachment, new FilterSinkHandler(buffer, handler))
    } else {
      val processed = filterOut(buffer.remaining, buffer)
      if (0 >= processed) {
        spill(attachment, new FilterCloseHandler(handler))
      } else if (flushing(handler)) {
        write(buffer, attachment, handler)
      } else {
        handler.completed(processed, attachment)
      }
    }
  }

  /**
   * @return true if the inner buffer has no more space left for writing
   */
  private[this] final def filled = {
    if (0 == innerbuffer.remaining)
      if (0 == innerbuffer.position && 0 == innerbuffer.limit) {
        innerbuffer.clear
        false
      } else true
    else false
  }

  private[this] final def flushing[A](handler: Handler[A]) = handler.isInstanceOf[FilterCloseHandler[A]]

  private[this] final def spill[A](attachment: A, handler: Handler[A]) = {
    innerbuffer.flip
    underlyingchannel.write(innerbuffer, attachment, handler)
  }

  private[this] final class FilterSinkHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 < innerbuffer.remaining) {
        underlyingchannel.write(innerbuffer, attachment, this)
      } else {
        innerbuffer.clear
        write(buffer, attachment, handler)
      }
    }

  }

  private[this] final class FilterCloseHandler[A](

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline final def completed(processed: Integer, attachment: A) = {
      if (0 < innerbuffer.remaining) {
        underlyingchannel.write(innerbuffer, attachment, this)
      } else {
        close
        handler.completed(0, attachment)
      }
    }

  }

}

/**
 *
 */
sealed trait FilterBaseConduit[C <: Channel]

  extends ConnectorConduit[C] {

  override final def isOpen = !closed

  override final def close = if (!closed) {
    closed = true
    releaseByteBuffer(innerbuffer)
    super.close
  }

  protected[this] final def skip(n: Int): Int = {
    val e = innerbuffer.position
    val skip = min(n, innerbuffer.remaining)
    innerbuffer.position(innerbuffer.position + skip)
    skip
  }

  protected[this] final val innerbuffer = { val b = defaultByteBuffer; b.flip; b }

  private[this] final var closed = false

}

