package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

/**
 * A conduit corresponding to /dev/null. You can read an infinite stream of (undeterministic) bytes from it or write any stream of bytes to it without any side effect.
 */
object NullConduit

  extends TerminatingSourceConduit

  with TerminatingSinkConduit {

  final def close = ()

  final def isOpen = true

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = readwrite(buffer, attachment, handler)

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = readwrite(buffer, attachment, handler)

  @inline private[this] final def readwrite[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    val e = buffer.remaining
    buffer.position(buffer.limit)
    handler.completed(e, attachment)
  }

}