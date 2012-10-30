package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler

class AsynchronousChannelTransfer[A] private (
  src: AsynchronousReadChannel,
  dst: AsynchronousWriteChannel,
  outerattachment: A,
  outerhandler: CompletionHandler[Long, _ >: A],
  buffer: ByteBuffer) {

  def transfer = {
    if (0 < buffer.remaining) {
      dst.write(buffer, Integer.valueOf(buffer.remaining), writehandler)
    } else {
      buffer.clear
      src.read(buffer, null, readhandler)
    }
  }

  type Integer = java.lang.Integer

  private[this] val writehandler = new CompletionHandler[Integer, Integer] {

    def completed(byteswritten: Integer, bytesread: Integer) = {
      try {
        transferred += byteswritten.longValue
        if (byteswritten < bytesread) {
          dst.write(buffer, Integer.valueOf(bytesread.intValue - byteswritten.intValue), this)
        } else {
          buffer.clear
          src.read(buffer, null, readhandler)
        }
      } catch { case e: Throwable ⇒ failed(e, null) }
    }

    def failed(e: Throwable, bytesread: Integer) = readhandler.failed(e, null)

  }

  private[this] val readhandler: CompletionHandler[Integer, Any] = new CompletionHandler[Integer, Any] {

    def completed(bytesread: Integer, attachment: Any) = {
      try {
        if (-1 < bytesread) {
          buffer.flip
          dst.write(buffer, bytesread, writehandler)
        } else {
          src.close
          if (null != outerhandler) outerhandler.completed(transferred, outerattachment)
        }
      } catch { case e: Throwable ⇒ failed(e, null) }
    }

    def failed(e: Throwable, attachment: Any) = {
      src.close
      if (null != outerhandler) outerhandler.failed(e, outerattachment)
    }

  }

  @volatile private[this] var transferred = 0L

}

object AsynchronousChannelTransfer {

  def apply[A](
    in: AsynchronousReadChannel,
    out: AsynchronousWriteChannel,
    attachment: A,
    handler: CompletionHandler[Long, _ >: A],
    buffer: ByteBuffer): Unit = new AsynchronousChannelTransfer(in, out, attachment, handler, buffer).transfer

  def apply[A](
    in: AsynchronousReadChannel,
    out: AsynchronousWriteChannel,
    attachment: A,
    handler: CompletionHandler[Long, _ >: A]): Unit =
    apply(in, out, attachment, handler, ByteBuffer.allocateDirect(defaultBufferSize))

  def apply[A](
    in: AsynchronousReadChannel,
    out: AsynchronousWriteChannel): Unit =
    apply(in, out, null, null, ByteBuffer.allocateDirect(defaultBufferSize))

}

