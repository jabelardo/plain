package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import text.ASCII

/**
 * A Renderable can put its content or fields into a CharBuffer.
 */
trait Renderable {

  def render(implicit buffer: ByteBuffer): Unit

  def +(that: Renderable)(implicit buffer: ByteBuffer): Renderable = {
    this.render(buffer)
    that
  }

}

/**
 * Basic constants used for rendering an http response.
 */
object Renderable {

  sealed abstract class SimpleRenderable

    extends Renderable {

    @inline final def render(implicit buffer: ByteBuffer) = buffer.put(bytes)

    final val bytes = toString.getBytes(ASCII)

  }

  object `\r\n` extends SimpleRenderable {

    @inline override final def +(that: Renderable)(implicit buffer: ByteBuffer): Renderable = {
      this.render(buffer)
      that.render(buffer)
      that
    }

  }

  object ` ` extends SimpleRenderable

  object `\t` extends SimpleRenderable

  object `:` extends SimpleRenderable

  case class r(bytes: Array[Byte]) extends Renderable {

    def this(s: String) = this(s.getBytes(ASCII))

    @inline final def render(implicit buffer: ByteBuffer) = buffer.put(bytes)

  }

}

import Renderable._

/**
 * The classic http response.
 */
case class Response(

  version: Version,

  status: Status,

  more: Any)

  extends Renderable {

  def this(status: Status) = this(Version.`HTTP/1.1`, status, None)

  @inline final def render(implicit buffer: ByteBuffer) = version + ` ` + status + `\r\n`

}

/**
 * The Response object.
 */
object Response {

  def apply(status: Status) = new Response(Version.`HTTP/1.1`, status, None)

}

