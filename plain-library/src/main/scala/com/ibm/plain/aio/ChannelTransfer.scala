package com.ibm

package plain

package aio

import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler }

import scala.util.continuations.suspendable

import aio.Io.{ read, write }

/**
 *
 */
final class ChannelTransfer private (

  src: Channel,

  dst: Channel,

  io: Io) {

  final def transfer: Io @suspendable = {

    import io._

    var finished = false

    @inline def writeloop: Any @suspendable = if (!finished) write(out)

    @inline def readloop: Any @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒ buffer.flip
      case _ ⇒ finished = true
    }

    if (0 == buffer.remaining) {
      while (!finished) {
        readloop
        writeloop
      }
    } else {
      while (!finished) {
        writeloop
        readloop
      }
    }

    buffer.limit(0)
    buffer.position(0)
    src match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    dst match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    io
  }

  final def transfer(encoder: Encoder): Io @suspendable = {

    import io._

    var finished = false

    @inline def writeloop: Any @suspendable = if (!finished) {
      encoder.encode(buffer);
      buffer.flip
      write(out)
    }

    @inline def readloop: Any @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒ buffer.flip
      case _ ⇒ finished = true
    }

    while (!finished) {
      readloop
      writeloop
    }
    encoder.finish(buffer)
    src match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    dst match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    io
  }

  private[this] final val in = io ++ src

  private[this] final val out = io ++ dst

}

object ChannelTransfer {

  def apply(src: Channel, dst: Channel, io: Io) = new ChannelTransfer(src, dst, io)

}
