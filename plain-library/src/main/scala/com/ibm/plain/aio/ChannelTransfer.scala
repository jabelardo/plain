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

  final def transfer: Unit @suspendable = {

    @inline def writeloop: Unit @suspendable = write(out) match {
      case out if out.readwritten == in.readwritten ⇒
        total += out.readwritten
        if (-1L == io.expected || total < io.expected) {
          readloop
        } else {
          src.close
          dst.close
        }
      case out ⇒
        total += out.readwritten
        writeloop
    }

    @inline def readloop: Unit @suspendable = read(in) match {
      case in if 0 < in.readwritten ⇒
        out.buffer.flip
        writeloop
        io.k(io)
      case in ⇒
        src.close
        dst.close
    }

    if (0 < out.buffer.remaining) writeloop else readloop
  }

  private[this] final var total = 0L

  private[this] final val in = { val i = Io.empty ++ src; i ++ io.buffer }

  private[this] final val out = { val o = Io.empty ++ dst; o ++ io.buffer }

}

object ChannelTransfer {

  def apply(src: Channel, dst: Channel, io: Io) = new ChannelTransfer(src, dst, io)

}

