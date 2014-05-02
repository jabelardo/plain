package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

/**
 *
 */
sealed trait FilterConduit {

  protected[this] def underlyingchannel: Channel

  def close = underlyingchannel.close

  def isOpen = underlyingchannel.isOpen

}

/**
 *
 */
trait FilterSourceConduit

  extends FilterConduit

  with SourceConduit {

  /**
   * Invariant: 0 < processed
   */
  protected[this] def filter(processed: Integer, buffer: ByteBuffer): Integer

  protected[this] def finish(buffer: ByteBuffer)

  @inline protected[this] final def doRead[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    underlyingchannel.read(innerbuffer, attachment, new FilterSourceHandler(buffer, handler))
  }

  @inline protected[this] final def doCompleted[A](processed: Integer, buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    handler.completed(filter(processed, buffer), attachment)
  }

  private[this] final class FilterSourceHandler[A](

    buffer: ByteBuffer,

    handler: Handler[A])

    extends BaseHandler[A](handler) {

    @inline def completed(processed: Integer, attachment: A) = {
      if (0 >= processed) {
        finish(buffer)
        handler.completed(processed, attachment)
      } else {
        innerbuffer.flip
        handler.completed(filter(processed, buffer), attachment)
      }
    }

  }

}

/**
 *
 */
trait FilterSinkConduit

  extends FilterConduit

  with SinkConduit {

}

