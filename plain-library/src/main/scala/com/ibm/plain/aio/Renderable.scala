package com.ibm

package plain

package aio

import java.nio.ByteBuffer

import concurrent.OnlyOnce
import text.`US-ASCII`

/**
 *
 */
trait RenderableRoot {

  def renderHeader(io: Io): Io

  def renderBody(io: Io): Io

  def renderFooter(io: Io): Io

}

/**
 * A Renderable can put its content or fields into an implicitly provided ByteBuffer.
 */
trait Renderable

  extends Any {

  def render(implicit buffer: ByteBuffer): Unit

  final def +(that: Renderable)(implicit buffer: ByteBuffer): Renderable = {
    this.render(buffer)
    that
  }

}

/**
 * Basic constants used for rendering an http response.
 */
object Renderable {

  case object `\r\n` extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = { buffer.put(text) }

    private[this] final val text = "\r\n".getBytes

  }

  sealed abstract class SimpleRenderable(

    private[this] final val byte: Byte)

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

  final class r private (val array: Array[Byte], val offset: Int, val length: Int)

    extends Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(array, offset, length)

  }

  object r {

    @inline final def apply(array: Array[Byte], offset: Int, length: Int): r = new r(array, offset, length)

    @inline final def apply(array: Array[Byte]): r = new r(array, 0, array.length)

    @inline final def apply(i: Int): r = apply(i.toString.getBytes)

    @inline final def apply(l: Long): r = apply(l.toString.getBytes)

  }

  final class rb private (val buffer: ByteBuffer)

    extends AnyVal with Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(buffer)

  }

  object rb {

    @inline final def apply(buffer: ByteBuffer): rb = new rb(buffer)

  }

}

