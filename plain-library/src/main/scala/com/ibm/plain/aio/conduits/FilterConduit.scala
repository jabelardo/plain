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
sealed trait FilterSourceConduit

  extends FilterConduit

  with SourceConduit {

  protected[this] def filter(processed: Integer, buffer: ByteBuffer): Integer

  @inline protected[this] final def doRead[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    underlyingchannel.read(conduitbuffer, attachment, new FilterSourceHandler(buffer, handler))
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
        handler.completed(processed, attachment)
      } else {
        conduitbuffer.flip
        handler.completed(filter(processed, buffer), attachment)
      }
    }

  }

}

/**
 *
 */
sealed trait FilterSinkConduit

  extends FilterConduit

  with SinkConduit {

}

/**
 *
 */
abstract class FilterByteChannel(

  protected[this] val underlyingchannel: Channel)

  extends FilterSourceConduit

  with FilterSinkConduit

/**
 *
 */
final class TestFilterChannel(underlying: Channel)

  extends FilterByteChannel(underlying) {

  final def hasSufficient = true

  final def filter(processed: Integer, buffer: ByteBuffer) = 0

}

