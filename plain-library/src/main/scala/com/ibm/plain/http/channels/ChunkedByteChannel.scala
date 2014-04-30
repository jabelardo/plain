package com.ibm

package plain

package http

package channels

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

import scala.math.min

import text.isHex
import aio._
import aio.Iteratee._
import aio.Input._
import aio.channels.FilterByteChannel

/**
 *
 */
final class ChunkedByteChannel private (

  underlyingchannel: Channel)

  extends FilterByteChannel(

    underlyingchannel) {

  protected[this] def onReadComplete[A](attachment: A, outerbuffer: ByteBuffer, outerhandler: Handler[Integer, _ >: A], innerbuffer: ByteBuffer, innerprocessed: Integer) = {
    if (0 < innerprocessed) {
      println("innerprocessed " + innerprocessed + " " + aio.format(innerbuffer))
      iteratee(Elem(exchange)) match {
        case (Done(chunk), input) ⇒ println("we have a chunk " + chunk)
        case e ⇒ println("we have a " + e)
      }
      outerhandler.completed(outerbuffer.remaining, attachment)
    } else {
      unsupported
    }
  }

  protected[this] def onWriteComplete[A](attachment: A, outerbuffer: ByteBuffer, outerhandler: Handler[Integer, _ >: A], innerbuffer: ByteBuffer, innerprocessed: Integer) = ()

  private[this] val exchange = Exchange(null, null, innerbuffer, innerbuffer)

  private[this] val iteratee = ChunkedIteratee.next

  //  private[this] final def nextChunk(innerbuffer: ByteBuffer): ByteBuffer = if (12 <= innerbuffer.remaining) {
  //    var c = innerbuffer.position
  //    if ('\r' == innerbuffer.get(c)) c += 1
  //    if ('\n' == innerbuffer.get(c)) c += 1
  //    val p = c
  //    while (isHex(innerbuffer.get(c))) c += 1
  //    val len = Integer.parseInt(new String(innerbuffer.array, p, c - p), 16)
  //    if ('\r' == innerbuffer.get(c)) c += 1
  //    if ('\n' == innerbuffer.get(c)) c += 1
  //    val chunk = ByteBuffer.wrap(innerbuffer.array, c, min(len, innerbuffer.remaining))
  //    innerbuffer.position(c + chunk.remaining)
  //    println("chunk " + chunk)
  //    println("inner " + innerbuffer)
  //    chunk
  //  } else unsupported

}

/**
 *
 */
object ChunkedByteChannel {

  final def apply(underlyingchannel: Channel) = new ChunkedByteChannel(underlyingchannel)

}