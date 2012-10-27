package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import scala.util.continuations.{ shift, suspendable }

import Io.{ IoCont, WriteHandler }
import Iteratee.Error
import text.ASCII

/**
 * A Renderable can put its content or fields into a ByteBuffer of an Io.
 */
trait Renderable {

  def render(implicit buffer: ByteBuffer): Unit

  def +(that: Renderable)(implicit buffer: ByteBuffer): Renderable = {
    this.render(buffer)
    that
  }

  private[aio] final def render(io: Io): Io @suspendable = {
    import io._
    shift { k: IoCont ⇒
      buffer.clear
      io ++ k
      try {
        render(buffer)
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

}

/**
 * Basic constants used for rendering an http response.
 */
object Renderable {

  case object `\r\n` extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = { buffer.put('\r'.toByte); buffer.put('\n'.toByte) }

  }

  sealed abstract class SimpleRenderable(

    byte: Byte)

    extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = buffer.put(byte)

  }

  /**
   * The 'flush' token for Renderables.
   */
  case object `@` extends Renderable { @inline final def render(implicit buffer: ByteBuffer) = () }

  case object ` ` extends SimpleRenderable(' '.toByte)

  case object `\t` extends SimpleRenderable('\t'.toByte)

  case object `:` extends SimpleRenderable(':'.toByte)

  final case class r(bytes: Array[Byte])

    extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = buffer.put(bytes)

  }

  object r {

    @inline final def apply(s: String) = new r(s.getBytes(ASCII))

  }

}

