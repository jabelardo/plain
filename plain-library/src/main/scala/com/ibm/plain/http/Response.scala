package com.ibm

package plain

package http

import java.nio.ByteBuffer
import javax.servlet.http.Cookie

import scala.language.implicitConversions

import aio.{ Transfer, Encoder, Io, RenderableRoot, releaseByteBuffer, tooTinyToCareSize }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._
import text.`UTF-8`
import time.{ now, rfc1123bytearray }
import Entity.{ ArrayEntity, AsynchronousByteChannelEntity, ByteBufferEntity }
import Message.Headers

/**
 * The classic http response.
 */
final case class Response private (

  request: Request,

  version: Version,

  var status: Status,

  var headers: Headers,

  var cookie: Cookie,

  var entity: Option[Entity])

  extends Message

  with RenderableRoot {

  import Response._

  type Type = Response

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  @inline final def ++(cookie: Cookie): Type = { this.cookie = cookie; this }

  @inline final def renderHeader(io: Io): Io = {
    implicit val _ = io
    bytebuffer = io.writebuffer
    renderVersion
    renderMandatory
    renderHeaders
    renderCookie
    renderKeepAlive(io)
    renderContent
    renderEntity(io)
  }

  @inline final def renderBody(io: Io): Io = {
    import io._
    @inline def encode = {
      encoding match {
        case Some(enc) ⇒
          enc.encode(writebuffer)
          enc.finish(writebuffer)
        case _ ⇒
      }
      io ++ Done[Io, Boolean](keepalive)
    }
    entity match {
      case Some(entity: AsynchronousByteChannelEntity) ⇒
        io ++ Transfer(entity.channel, channel, encoding) ++ Done[Io, Boolean](keepalive)
      case Some(entity: ByteBufferEntity) ⇒
        markbuffer = writebuffer
        writebuffer = entity.buffer
        encode
      case Some(ArrayEntity(array, offset, length, _)) ⇒
        markbuffer = writebuffer
        writebuffer = ByteBuffer.wrap(array, offset, length.toInt)
        encode
      case _ ⇒ unsupported
    }
  }

  @inline final def renderFooter(io: Io): Io = {
    import io._
    if (null != markbuffer) {
      releaseByteBuffer(writebuffer)
      writebuffer = markbuffer
      markbuffer = null
    }
    io
  }

  @inline private[this] final def renderVersion = {
    version + ` ` + status + `\r\n` + ^
  }

  @inline private[this] final def renderMandatory = {
    r(`Server: `) + r(`Date: `) + r(rfc1123bytearray) + `\r\n` + ^
  }

  @inline private[this] final def renderHeaders = if (null != headers) headers.foreach {
    case (name, value) ⇒ r(name.getBytes(`UTF-8`)) + `:` + ` ` + r(value.getBytes(`UTF-8`)) + `\r\n` + ^
  }

  @inline private[this] final def renderCookie = if (null != cookie) {
    r(`Set-Cookie: `) + rc(cookie) + `\r\n` + ^
  }

  @inline private[this] final def renderKeepAlive(io: Io) = {
    val keepalive = null == request || request.keepalive
    io ++ keepalive
    if (!keepalive) r(`Connection: close`) + ^
  }

  @inline private[this] final def renderContent: Unit = {
    encoding = entity match {
      case Some(entity) if entity.contenttype.mimetype.encodable ⇒ request.transferEncoding
      case _ ⇒ None
    }
    entity match {
      case Some(entity) ⇒
        r(`Content-Type: `) + entity.contenttype + `\r\n` + ^
        encoding match {
          case Some(encoding) ⇒
            r(`Content-Encoding: `) + r(encoding.text) + `\r\n` + ^
            r(`Transfer-Encoding: chunked`) + ^
          case _ ⇒
            r(`Content-Length: `) + r(entity.length) + `\r\n` + `\r\n` + ^
        }
      case _ ⇒ `\r\n` + ^
    }
  }

  @inline private[this] final def renderEntity(io: Io): Io = {
    import io._
    @inline def encode(entity: Entity) = {
      encoding match {
        case Some(enc) ⇒
          writebuffer.limit(writebuffer.position)
          writebuffer.position(writebuffer.position - entity.length.toInt)
          enc.encode(writebuffer)
          enc.finish(writebuffer)
          writebuffer.position(writebuffer.limit)
          writebuffer.limit(writebuffer.capacity)
        case _ ⇒
      }
      io ++ Done[Io, Boolean](keepalive)
    }
    entity match {
      case Some(entity: ByteBufferEntity) if entity.length <= writebuffer.remaining ⇒
        rb(entity.buffer) + ^
        releaseByteBuffer(entity.buffer)
        encode(entity)
      case Some(entity @ ArrayEntity(array, offset, length, _)) if length <= writebuffer.remaining ⇒
        r(array, offset, length.toInt) + ^
        encode(entity)
      case Some(_) ⇒
        io ++ Cont[Io, Boolean](null)
      case None ⇒
        io ++ Done[Io, Boolean](keepalive)
    }
  }

  private[this] final def rc(cookie: Cookie) = r((cookie.getName + "=" + cookie.getValue + (cookie.getPath match { case null ⇒ "" case path ⇒ "; Path=" + path }) + (if (cookie.isHttpOnly) "; HttpOnly" else "")).getBytes(`UTF-8`))

  private[this] final var encoding: Option[Encoder] = None

  private[this] final var markbuffer: ByteBuffer = null

  private[this] final implicit var bytebuffer: ByteBuffer = null

}

/**
 * The Response object.
 */
object Response {

  final def apply(request: Request, status: Status) = new Response(request, Version.`HTTP/1.1`, status, null, null, None)

  final def apply(request: Request, status: Status, headers: Headers) = new Response(request, Version.`HTTP/1.1`, status, headers, null, None)

  private final val `Connection: keep-alive` = "Connection: keep-alive\r\n".getBytes

  private final val `Connection: close` = "Connection: close\r\n".getBytes

  private final val `Set-Cookie: ` = "Set-Cookie: ".getBytes

  private final val `Content-Type: ` = "Content-Type: ".getBytes

  private final val `Content-Encoding: ` = "Content-Encoding: ".getBytes

  private final val `Content-Length: ` = "Content-Length: ".getBytes

  private final val `Transfer-Encoding: chunked` = "Transfer-Encoding: chunked\r\n".getBytes

  private final val `Server: ` = ("Server: plain " + config.version + "\r\n").getBytes

  private final val `Date: ` = "Date: ".getBytes

  private final val `Path=` = "Path=".getBytes

  private final val `HttpOnly` = "HttpOnly".getBytes

}

