package com.ibm

package plain

package aio

package conduit

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

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    val e = buffer.remaining
    while (0 < buffer.remaining) buffer.put(nul, 0, min(nul.length, buffer.remaining))
    buffer.position(buffer.limit)
    handler.completed(e, attachment)
  }

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    val e = buffer.remaining
    buffer.position(buffer.limit)
    handler.completed(e, attachment)
  }

  final val nul = Array.fill(2048)(0.toByte)

}
