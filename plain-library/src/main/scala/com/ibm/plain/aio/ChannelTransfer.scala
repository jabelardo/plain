package com.ibm

package plain

package aio

import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

import scala.util.continuations.suspendable

import aio.Io.{ read, write }
import logging.HasLogger

/**
 *
 */
final class ChannelTransfer private (

  src: Channel,

  dst: Channel,

  io: Io) {

  final def transfer: Io @suspendable = {

    import io._

    @inline def writeloop: Unit @suspendable = write(out) match { case _ ⇒ readloop }

    @inline def readloop: Unit @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒
        buffer.flip
        writeloop
      case _ ⇒
    }

    if (0 < buffer.remaining) writeloop else readloop

    buffer.limit(0)
    buffer.position(0)
    src match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    dst match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    io
  }

  final def transfer(compressor: Compressor): Io @suspendable = {

    import io._

    @inline def writeloop: Unit @suspendable = {
      compressor.compress(buffer);
      buffer.flip
      write(out) match { case _ ⇒ readloop }
    }

    @inline def readloop: Unit @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒
        buffer.flip
        writeloop
      case _ ⇒
    }

    if (0 < buffer.remaining) writeloop else readloop

    compressor.finish(buffer)
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
