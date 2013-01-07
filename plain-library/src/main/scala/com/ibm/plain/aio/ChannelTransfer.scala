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
  arr: Array[Byte],
  io: Io,
  outerhandler: Handler[Long, Io]) {

  private def this(src: ReadChannel, arr: Array[Byte], io: Io, outerhandler: Handler[Long, Io]) =
    this(src, null, arr, io, outerhandler)

  @inline private final def transfer: Nothing = {
    import io._
    if (0 < buffer.remaining) {
      arr match {
        case null ⇒ dst.write(buffer, Integer.valueOf(buffer.remaining), writehandler)
        case arr ⇒ val l = Integer.valueOf(buffer.remaining); writehandler.completed(l, l)
      }
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
          if (null != arr) buffer.get(arr, transferred.toInt, byteswritten.intValue)
          transferred += byteswritten.longValue
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
          arr match {
            case null ⇒ dst.write(buffer, bytesread, writehandler)
            case arr ⇒ val l = Integer.valueOf(bytesread); writehandler.completed(l, l)
          }
        } else {
          outerhandler.completed(transferred, io)
        }
      } catch {
        case ControlCompleted ⇒
        case e: Throwable ⇒ failed(e, null)
      }
    }

    def failed(e: Throwable, io: Io) = {
      import io._
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
    handler: Handler[Long, Io]): Nothing = new ChannelTransfer(in.channel, out, null, in, handler).transfer

  def apply(
    in: ReadChannel,
    out: Io,
    handler: Handler[Long, Io]): Nothing = new ChannelTransfer(in, out.channel, null, out, handler).transfer

  def apply(
    in: Io,
    out: Array[Byte],
    handler: Handler[Long, Io]): Nothing = new ChannelTransfer(in.channel, null, out, in, handler).transfer

}

