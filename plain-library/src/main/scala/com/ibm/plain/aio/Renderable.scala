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

  final class r private (val array: Array[Byte])

    extends AnyVal with Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(array)

  }

  object r {

    @inline final def apply(array: Array[Byte]): r = new r(array)

    @inline final def apply(l: Long): r = new r(ls(l))

    @inline final def apply(i: Int): r = new r(ls(i))

    @inline private[this] final def ls(l: Long) = try longs(l.toInt) catch { case _: Throwable ⇒ l.toString.getBytes }

    @inline private[this] final def ls(i: Int) = try longs(i) catch { case _: Throwable ⇒ i.toString.getBytes }

    private[this] final val longs: Array[Array[Byte]] = Array(
      "0".getBytes,
      "1".getBytes,
      "2".getBytes,
      "3".getBytes,
      "4".getBytes,
      "5".getBytes,
      "6".getBytes,
      "7".getBytes,
      "8".getBytes,
      "9".getBytes,
      "10".getBytes,
      "11".getBytes,
      "12".getBytes,
      "13".getBytes,
      "14".getBytes,
      "15".getBytes,
      "16".getBytes,
      "17".getBytes,
      "18".getBytes,
      "19".getBytes,
      "20".getBytes,
      "21".getBytes,
      "22".getBytes,
      "23".getBytes,
      "24".getBytes,
      "25".getBytes,
      "26".getBytes,
      "27".getBytes,
      "28".getBytes,
      "29".getBytes,
      "30".getBytes,
      "31".getBytes,
      "32".getBytes)

  }

  final class rb private (val buffer: ByteBuffer)

    extends AnyVal with Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(buffer)

  }

  object rb {

    @inline final def apply(buffer: ByteBuffer): rb = new rb(buffer)

  }

}

