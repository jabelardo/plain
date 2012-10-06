package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler, AsynchronousByteChannel }
import java.nio.ByteBuffer

class AsynchronousChannelTransfer[A] private (
  src: AsynchronousByteChannel,
  dst: AsynchronousByteChannel,
  outerattachment: A,
  outerhandler: CompletionHandler[Long, _ >: A],
  buffer: ByteBuffer)

  extends logging.HasLogger {

  def transfer = {
    buffer.clear
    src.read(buffer, null, readhandler)
  }

  type Integer = java.lang.Integer

  private[this] val writehandler = new CompletionHandler[Integer, Integer] {

    def completed(byteswritten: Integer, bytesread: Integer) = {
      try {
        transferred += byteswritten.longValue
        if (byteswritten < bytesread) {
          warning("Not written all, need more write " + byteswritten + " < " + bytesread + " " + buffer)
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
          outerhandler.completed(transferred, outerattachment)
        }
      } catch { case e: Throwable ⇒ failed(e, null) }
    }

    def failed(e: Throwable, attachment: Any) = {
      src.close
      outerhandler.failed(e, outerattachment)
    }

  }

  @volatile private[this] var transferred = 0L

}

object AsynchronousChannelTransfer {

  def transfer[A](
    in: AsynchronousByteChannel,
    out: AsynchronousByteChannel,
    attachment: A,
    handler: CompletionHandler[Long, _ >: A],
    buffer: ByteBuffer): Unit = new AsynchronousChannelTransfer(in, out, attachment, handler, buffer).transfer

  def transfer[A](
    in: AsynchronousByteChannel,
    out: AsynchronousByteChannel,
    attachment: A,
    handler: CompletionHandler[Long, _ >: A]): Unit =
    transfer(in, out, attachment, handler, ByteBuffer.allocateDirect(defaultBufferSize))

}

