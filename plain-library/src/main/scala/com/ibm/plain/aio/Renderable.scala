package com.ibm

package plain

package aio

import java.nio.ByteBuffer

import scala.util.continuations.suspendable

import concurrent.OnlyOnce
import text.`US-ASCII`

/**
 *
 */
trait RenderableRoot {

  def renderHeader(io: Io): Io

  def renderBody(io: Io): Io @suspendable

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

    @inline private[this] final def ls(l: Long) = l match {
      case 0 ⇒ `0`
      case 1 ⇒ `1`
      case 2 ⇒ `2`
      case 3 ⇒ `3`
      case 4 ⇒ `4`
      case 5 ⇒ `5`
      case 6 ⇒ `6`
      case 7 ⇒ `7`
      case 8 ⇒ `8`
      case 9 ⇒ `9`
      case 10 ⇒ `10`
      case l ⇒ l.toString.getBytes
    }

    private[this] final val `0` = "0".getBytes
    private[this] final val `1` = "1".getBytes
    private[this] final val `2` = "2".getBytes
    private[this] final val `3` = "3".getBytes
    private[this] final val `4` = "4".getBytes
    private[this] final val `5` = "5".getBytes
    private[this] final val `6` = "6".getBytes
    private[this] final val `7` = "7".getBytes
    private[this] final val `8` = "8".getBytes
    private[this] final val `9` = "9".getBytes
    private[this] final val `10` = "10".getBytes

  }

  final class rb private (val buffer: ByteBuffer)

    extends AnyVal with Renderable {

    @inline final def render(implicit out: ByteBuffer) = out.put(buffer)

  }

  object rb {

    @inline final def apply(buffer: ByteBuffer): rb = new rb(buffer)

  }

}

