package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

/**
 *
 */
final class ChannelTransfer private (
  src: ReadChannel,
  dst: WriteChannel,
  io: Io,
  outerhandler: Handler[Long, Io]) {

  @inline private final def transfer: Nothing = {
    import io._
    if (0 < buffer.remaining) {
      dst.write(buffer, Integer.valueOf(buffer.remaining), writehandler)
    } else {
      buffer.clear
      src.read(buffer, io, readhandler)
    }
    throw AioDone
  }

  type Integer = java.lang.Integer

  private[this] final val writehandler: Handler[Integer, Integer] = new Handler[Integer, Integer] {

    def completed(byteswritten: Integer, bytesread: Integer) = {
      import io._
      try {
        transferred += byteswritten.longValue
        if (byteswritten < bytesread) {
          dst.write(buffer, Integer.valueOf(bytesread.intValue - byteswritten.intValue), this)
        } else {
          buffer.clear
          if (-1 == io.expected || transferred < io.expected) {
            buffer.clear
            src.read(buffer, io, readhandler)
          } else {
            readhandler.completed(Integer.valueOf(-1), io)
          }
        }
      } catch { case e: Throwable ⇒ failed(e, null) }
    }

    def failed(e: Throwable, bytesread: Integer) = readhandler.failed(e, io)

  }

  private[this] final val readhandler = new Handler[Integer, Io] {

    def completed(bytesread: Integer, io: Io) = {
      import io._
      try {
        if (-1 < bytesread) {
          buffer.flip
          dst.write(buffer, bytesread, writehandler)
        } else {
          outerhandler.completed(transferred, io)
        }
      } catch { case e: Throwable ⇒ failed(e, null) }
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      outerhandler.failed(e, io)
    }

  }

  @volatile private[this] var transferred = 0L

}

object ChannelTransfer {

  def apply(
    in: Io,
    out: WriteChannel,
    handler: Handler[Long, Io]) = new ChannelTransfer(in.channel, out, in, handler).transfer

  def apply(
    in: ReadChannel,
    out: Io,
    handler: Handler[Long, Io]) = new ChannelTransfer(in, out.channel, out, handler).transfer

}

