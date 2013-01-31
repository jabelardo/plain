package com.ibm

package plain

package aio

import java.nio.{ BufferOverflowException, ByteBuffer }
import java.util.concurrent.TimeUnit

import scala.util.continuations.{ reset, shift, suspendable }

import Io.{ IoCont, WriteHandler }
import Iteratee.{ Done, Error }
import Iteratee.Error
import text.`US-ASCII`
import concurrent.OnlyOnce

/**
 * A Renderable can put its content or fields into a ByteBuffer of an Io.
 */
trait Renderable {

  def render(implicit buffer: ByteBuffer): Unit

  def doRender(io: Io): Iteratee[Long, Boolean] = null

  final def +(that: Renderable)(implicit buffer: ByteBuffer): Renderable = {
    this.render(buffer)
    that
  }

}

/**
 * Basic constants used for rendering an http response.
 */
object Renderable

  extends OnlyOnce {

  case object `\r\n` extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = { buffer.put(crlf) }

    private[this] final val crlf = "\r\n".getBytes

  }

  sealed abstract class SimpleRenderable(

    byte: Byte)

    extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = buffer.put(byte)

  }

  /**
   * The 'flush' token for Renderables, sequences of + must end with a ^.
   */
  case object ^ extends Renderable { @inline final def render(implicit buffer: ByteBuffer) = () }

  case object ` ` extends SimpleRenderable(' '.toByte)

  case object `\t` extends SimpleRenderable('\t'.toByte)

  case object `:` extends SimpleRenderable(':'.toByte)

  final class r private (buffer: ByteBuffer)

    extends Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(buffer)

  }

  object r {

    @inline final def apply(buffer: ByteBuffer) = new r(buffer)

    @inline final def apply(a: Array[Byte]): r = apply(ByteBuffer.wrap(a))

    @inline final def apply(s: String): r = apply(s.getBytes(`US-ASCII`))

  }

  private def warning = onlyonce {
    logging.defaultLogger.warning("The aio.defaultBufferSize is too small to hold an entire http response header and should be enlarged: " + defaultBufferSize)
  }

  private def fatal = {
    val msg = "The aio.defaultBufferSize is too small to hold a single part of the http response header and must be enlarged: " + defaultBufferSize
    logging.defaultLogger.error(msg)
    throw new Exception(msg)
  }

}

