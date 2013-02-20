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

    @inline def writeloop: Unit @suspendable = write(out) match {
      case out ⇒
        total += out.readwritten
        if (-1L == io.expected || total < expected) readloop
    }

    @inline def readloop: Unit @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒
        writeloop
      case _ ⇒
    }

    if (0 < buffer.remaining) writeloop else readloop

    buffer.clear
    src match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    dst match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
    io
  }

  private[this] final var total = 0L

  private[this] final val in = io ++ src

  private[this] final val out = io ++ dst

}

object ChannelTransfer {

  def apply(src: Channel, dst: Channel, io: Io) = new ChannelTransfer(src, dst, io)

}

