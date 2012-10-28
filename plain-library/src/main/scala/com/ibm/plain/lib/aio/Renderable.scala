package com.ibm.plain

package lib

package aio

import java.nio.{ BufferOverflowException, ByteBuffer }
import java.util.concurrent.TimeUnit

import scala.util.continuations.{ reset, shift, suspendable }

import Io.{ IoCont, WriteHandler }
import Iteratee.Error
import text.ASCII

/**
 * A Renderable can put its content or fields into a ByteBuffer of an Io.
 */
trait Renderable {

  def render(implicit io: Io): Unit

  def +(that: Renderable)(implicit io: Io): Renderable = {
    import io._
    try {
      this.render(io)
    } catch {
      case e: BufferOverflowException if buffer.remaining == buffer.capacity ⇒
        buffer.clear
        fatal
      case e: BufferOverflowException ⇒
        warning
        buffer.flip
        reset { write(io); () }
        buffer.clear
        this + that
      case e: Throwable ⇒ throw e
    }
    that
  }

  private[aio] final def doRender(io: Io): Io @suspendable = {
    import io._
    shift { k: IoCont ⇒
      buffer.clear
      io ++ k
      try {
        render(io)
        buffer.flip
        k(io)
      } catch {
        case e: Throwable ⇒ k(io ++ Error[Io](e))
      }
    }
    write(io)
  }

  private[this] final def write(io: Io): Io @suspendable = {
    import io._
    shift { k: IoCont ⇒ channel.write(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, io ++ k, WriteHandler) }
    if (0 < io.buffer.remaining) write(io) else shift { k: IoCont ⇒ io ++ k; k(io) }
  }

  private[this] def warning = {
    logging.defaultLogger.warning("The aio.defaultBufferSize is too small to hold an entire http response header and should be enlarged: " + defaultBufferSize)
  }

  private[this] def fatal = {
    val msg = "The aio.defaultBufferSize is too small to hold a single part of the http response header and must be enlarged: " + defaultBufferSize
    logging.defaultLogger.error(msg)
    throw new Exception(msg)
  }

}

/**
 * Basic constants used for rendering an http response.
 */
object Renderable {

  case object `\r\n` extends Renderable {

    @inline final def render(implicit io: Io) = { io.buffer.put('\r'.toByte); io.buffer.put('\n'.toByte) }

  }

  sealed abstract class SimpleRenderable(

    byte: Byte)

    extends Renderable {

    @inline final def render(implicit io: Io) = io.buffer.put(byte)

  }

  /**
   * The 'flush' token for Renderables, sequences of + must end with a ^.
   */
  case object ^ extends Renderable { @inline final def render(implicit io: Io) = () }

  case object ` ` extends SimpleRenderable(' '.toByte)

  case object `\t` extends SimpleRenderable('\t'.toByte)

  case object `:` extends SimpleRenderable(':'.toByte)

  final case class r(bytes: Array[Byte])

    extends Renderable {

    @inline final def render(implicit io: Io) = io.buffer.put(bytes)

  }

  object r {

    @inline final def apply(s: String) = new r(s.getBytes(ASCII))

  }

}

