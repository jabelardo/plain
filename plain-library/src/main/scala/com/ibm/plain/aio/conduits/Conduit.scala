package com.ibm

package plain

package aio

package conduits

import java.nio.{ BufferOverflowException, ByteBuffer }
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

import scala.math.min

/**
 *
 */
sealed trait Conduit

  extends Channel {

  final def read(buffer: ByteBuffer) = unsupported

  final def write(buffer: ByteBuffer) = unsupported

  type Handler[A] = CompletionHandler[Integer, _ >: A]

  protected[this] abstract class BaseHandler[A](handler: Handler[A])

    extends CompletionHandler[Integer, A] {

    final def failed(e: Throwable, attachment: A) = { println("failed conduit " + e); handler.failed(e, attachment) }

  }

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
trait SourceConduit

  extends Conduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isDrained) {
      innerbuffer.clear
      doRead(buffer, attachment, handler)
    } else {
      if (checkSufficient) {
        doCompleted(innerbuffer.remaining, buffer, attachment, handler)
      } else {
        handleOverflow
        doRead(buffer, attachment, handler)
      }
    }

  }

  protected[this] def doRead[A](buffer: ByteBuffer, attachment: A, handler: Handler[A])

  protected[this] def doCompleted[A](processed: Integer, buffer: ByteBuffer, attachment: A, handler: Handler[A])

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

}

/**
 *
 */
trait SinkConduit

  extends Conduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {}

}

