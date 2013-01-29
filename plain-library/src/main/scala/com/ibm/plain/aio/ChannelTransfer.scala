package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

/**
 *
 */
final class ChannelTransfer private (
  src: ReadChannel,
  dst: WriteChannel,
  io: Io,
  iodestination: Boolean,
  outerhandler: Handler[Long, Io]) {

  @inline private final def transfer: Nothing = {
    import io._
    if (iodestination) buffer.flip
    if (0 < buffer.remaining) {
      dst.write(buffer, Integer.valueOf(buffer.remaining), writehandler)
    } else {
      buffer.clear
      src.read(buffer, io, readhandler)
    }
    throw ControlCompleted
  }

  type Integer = java.lang.Integer

  private[this] final val writehandler: Handler[Integer, Integer] = new Handler[Integer, Integer] {

    def completed(byteswritten: Integer, bytesread: Integer) = {
      import io._
      try {
        if (byteswritten < bytesread) {
          transferred += byteswritten.longValue
          dst.write(buffer, Integer.valueOf(bytesread.intValue - byteswritten.intValue), this)
        } else {
          transferred += byteswritten.longValue
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
          src.close
          dst.close
          outerhandler.completed(transferred, io)
        }
      } catch {
        case ControlCompleted ⇒
        case e: Throwable ⇒ failed(e, null)
      }
    }

    def failed(e: Throwable, io: Io) = {
      import io._
      src.close
      dst.close
      outerhandler.failed(e, io)
    }

  }

  @volatile private[this] var transferred = 0L

}

object ChannelTransfer {

  /**
   * Careful, apply returns Nothing, thus, a Handler for completed or failed is mandatory. Please note that any line following apply will be dead code.
   */
  def apply(
    in: Io,
    out: WriteChannel,
    handler: Handler[Long, Io]): Nothing = new ChannelTransfer(ReadChannel(in.channel, false), out, in, false, handler).transfer

  def apply(
    in: ReadChannel,
    out: Io,
    handler: Handler[Long, Io]): Nothing = new ChannelTransfer(in, WriteChannel(out.channel, false), out, true, handler).transfer

}

