package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

/**
 *
 */
sealed trait Conduit

  extends Channel {

  final def read(buffer: ByteBuffer) = unsupported

  final def write(buffer: ByteBuffer) = unsupported

  type Handler[A] = CompletionHandler[Integer, _ >: A]

  protected[this] val conduitbuffer = { val b = ByteBuffer.wrap(new Array[Byte](defaultBufferSize)); b.flip; b }

  protected[this] abstract class BaseHandler[A](handler: Handler[A])

    extends CompletionHandler[Integer, A] {

    final def failed(e: Throwable, attachment: A) = handler.failed(e, attachment)

  }

}

/**
 *
 */
trait SourceConduit

  extends Conduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (isDrained) {
      conduitbuffer.clear
      doRead(buffer, attachment, handler)
    } else {
      if (checkSufficient) {
        doCompleted(conduitbuffer.remaining, buffer, attachment, handler)
      } else {
        val overflow = ByteBuffer.wrap(conduitbuffer.array, 0, conduitbuffer.position)
        require(overflow.remaining >= conduitbuffer.remaining, throw new java.nio.BufferOverflowException)
        overflow.put(conduitbuffer)
        overflow.flip
        conduitbuffer.position(overflow.limit)
        conduitbuffer.limit(conduitbuffer.capacity)
        doRead(buffer, attachment, handler)
      }
    }

  }

  protected[this] def doRead[A](buffer: ByteBuffer, attachment: A, handler: Handler[A])

  protected[this] def doCompleted[A](processed: Integer, buffer: ByteBuffer, attachment: A, handler: Handler[A])

  protected[this] def hasSufficient: Boolean

  protected[this] final def isDrained = 0 == conduitbuffer.remaining

  protected[this] final def available = conduitbuffer.remaining

  private[this] final def checkSufficient = !isDrained && hasSufficient

}

/**
 *
 */
trait SinkConduit

  extends Conduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {}

}

