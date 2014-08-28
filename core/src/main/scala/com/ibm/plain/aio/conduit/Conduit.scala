package com.ibm

package plain

package aio

package conduit

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }
import java.util.concurrent.Future

/**
 * A Conduit is an abstraction of an AsynchronousByteChannel. Note: Not of AsynchronousChannel. Classes directly derived from AsynchronousChannel need to be wrapped to become a Conduit.
 */
trait Conduit

    extends Channel {

  type Handler[A] = CompletionHandler[Integer, _ >: A]

  protected[this] abstract class BaseHandler[A](

    private[this] final val handler: Handler[A])

      extends CompletionHandler[Integer, A] {

    def failed(e: Throwable, attachment: A) = handler.failed(e, attachment)

  }

}

/**
 * A Conduit is split into its source for reading from it and its sink for writing to it. SourceConduit builds the base for read implementations.
 */
trait SourceConduit extends Conduit {

  def read(buffer: ByteBuffer): Future[Integer] = unsupported

  def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A])

}

/**
 * A Conduit is split into its source for reading from it and its sink for writing to it. SinkConduit builds the base for write implementations.
 */
trait SinkConduit extends Conduit {

  def write(buffer: ByteBuffer): Future[Integer] = unsupported

  def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A])

}

