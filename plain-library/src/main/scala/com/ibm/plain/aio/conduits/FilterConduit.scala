package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.{ BufferOverflowException, ByteBuffer }
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }
import java.util.concurrent.atomic.AtomicBoolean

import scala.math.min

/**
 * A FilterConduit modifies or manipulates the data of its underlying channel during reads and writes.
 */
trait FilterSourceConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with SourceConduit[C] {

  protected[this] def filterIn(processed: Integer, buffer: ByteBuffer): Integer

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isDrained) {
      innerbuffer.clear
      underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
    } else {
      if (hasSufficient) {
        handler.completed(filterIn(innerbuffer.remaining, buffer), attachment)
      } else {
        handleOverflow
        underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
      }
    }

  }

  protected[this] def hasSufficient: Boolean

  protected[this] final def available = innerbuffer.remaining

  private[this] final def isDrained = 0 == innerbuffer.remaining

  private[this] final def handleOverflow = {
    val overflow = ByteBuffer.wrap(innerbuffer.array, 0, innerbuffer.position)
    require(overflow.remaining >= innerbuffer.remaining, throw new BufferOverflowException)
    overflow.put(innerbuffer)
    overflow.flip
    innerbuffer.position(overflow.limit)
    innerbuffer.limit(innerbuffer.capacity)
  }

  private[this] final class FilterSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    final def completed(processed: Integer, attachment: A) = {
      if (0 < innerbuffer.position) innerbuffer.flip
      handler.completed(filterIn(processed, buffer), attachment)
    }

  }

}

/**
 *
 */
trait FilterSinkConduit[C <: Channel]

  extends FilterBaseConduit[C]

  with SinkConduit[C] {

  protected[this] def filterOut(processed: Integer, buffer: ByteBuffer): Integer

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isFilled) {
      doWrite(attachment, new FilterSinkHandler(buffer, handler))
    } else {
      val len = filterOut(buffer.remaining, buffer)
      if (0 < len) {
        handler.completed(len, attachment)
      } else if (0 < innerbuffer.remaining) {
        doWrite(attachment, new FilterCloseHandler(handler))
      } else {
        close
      }
    }
  }

  private[this] final def isFilled = {
    if (0 == innerbuffer.remaining)
      if (0 == innerbuffer.position && 0 == innerbuffer.limit) {
        innerbuffer.clear
        false
      } else true
    else false
  }

  private[this] final def doWrite[A](attachment: A, handler: Handler[A]) = {
    innerbuffer.flip
    underlyingchannel.write(innerbuffer, attachment, handler)
  }

  private[this] final class FilterSinkHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A])

    extends BaseHandler[A](handler) {

    final def completed(processed: Integer, attachment: A) = {
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

    final def completed(processed: Integer, attachment: A) = {
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

  extends Conduit[C] {

  override final def close = if (!isclosed) {
    isclosed = true
    releaseByteBuffer(innerbuffer)
    super.close
  }

  protected[this] final def skip(n: Int): Int = {
    val e = innerbuffer.position
    val skip = min(n, innerbuffer.remaining)
    innerbuffer.position(innerbuffer.position + skip)
    skip
  }

  protected[this] val innerbuffer = { val b = defaultByteBuffer; b.flip; b }

  protected[this] final var isclosed = false

}

