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
trait Conduit[C <: Channel]

  extends Channel {

  def close = underlyingchannel.close

  def isOpen = underlyingchannel.isOpen

  final def read(buffer: ByteBuffer) = unsupported

  final def write(buffer: ByteBuffer) = unsupported

  type Handler[A] = CompletionHandler[Integer, _ >: A]

  protected[this] def underlyingchannel: C

  protected[this] abstract class BaseHandler[A](handler: Handler[A])

    extends CompletionHandler[Integer, A] {

    final def failed(e: Throwable, attachment: A) = handler.failed(e, attachment)

  }

}

/**
 *
 */
trait SourceConduit[C <: Channel]

  extends Conduit[C]

/**
 *
 */
trait SinkConduit[C <: Channel]

  extends Conduit[C]
