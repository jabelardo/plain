package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

import scala.math.min

/**
 * A conduit corresponding to /dev/null. You can read an infinite stream of nul bytes from it or write any stream of bytes to it without any side effect.
 */
object NullConduit

  extends TerminatingSourceConduit

  with TerminatingSinkConduit {

  final def close = ()

  final def isOpen = true

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = readwrite(buffer, attachment, handler, true)

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = readwrite(buffer, attachment, handler, false)

  @inline private[this] final def readwrite[A](buffer: ByteBuffer, attachment: A, handler: Handler[A], nullify: Boolean) = {
    val e = buffer.remaining
    if (nullify) while (0 < buffer.remaining) buffer.put(nulls, 0, min(nulls.length, buffer.remaining))
    buffer.position(buffer.limit)
    handler.completed(e, attachment)
  }

  final val nulls = Array.fill(2048)(0.toByte)

}
