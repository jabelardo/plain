package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

/**
 * A Conduit is an abstraction of an AsynchronousByteChannel. Note: Not of AsynchronousChannel. Classes directly derived from it need to be wrapped to become a Conduit.
 */
trait Conduit[C <: Channel]

  extends Channel {

  def close = underlyingchannel.close

  def isOpen = underlyingchannel.isOpen

  final def read(buffer: ByteBuffer) = unsupported

  final def write(buffer: ByteBuffer) = unsupported

  type Handler[A] = CompletionHandler[Integer, _ >: A]

  protected[this] def underlyingchannel: C

  protected[this] abstract class BaseHandler[A](

    private[this] final val handler: Handler[A])

    extends CompletionHandler[Integer, A] {

    def failed(e: Throwable, attachment: A) = handler.failed(e, attachment)

  }

}

/**
 * A Conduit is split into its source for reading from it and sink for writing to it. SourceConduit builds the base for reading implementations.
 */
trait SourceConduit[C <: Channel]

  extends Conduit[C]

/**
 * A Conduit is split into its source for reading from it and sink for writing to it. SinnkConduit builds the base for writing implementations.
 */
trait SinkConduit[C <: Channel]

  extends Conduit[C]
