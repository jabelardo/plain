package com.ibm

package plain

package http

import java.nio.ByteBuffer

import aio.{ Transfer, Encoder, Io, RenderableRoot, releaseByteBuffer, tooTinyToCareSize }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._
import text.`UTF-8`
import Entity.{ ArrayEntity, AsynchronousByteChannelEntity, ByteBufferEntity }
import Message.Headers

/**
 * The classic http response.
 */
final case class Response private (

  request: Request,

  version: Version,

  var status: Status,

  val headers: Headers,

  var entity: Option[Entity])

  extends Message

  with RenderableRoot {

  import Response._

  type Type = Response

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def renderHeader(io: Io): Io = {
    implicit val _ = io
    bytebuffer = io.writebuffer
    renderVersion
    renderServer
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
      case Some(entity: ArrayEntity) ⇒
        markbuffer = writebuffer
        writebuffer = ByteBuffer.wrap(entity.array)
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

  @inline private[this] final def renderServer = {
    r(`Server: `) + `\r\n` + ^
  }

  @inline private[this] final def renderKeepAlive(io: Io) = {
    val keepalive = null == request || request.keepalive
    io ++ keepalive
    r(`Connection: `) + r(if (keepalive) `keep-alive` else `close`) + `\r\n` + ^
  }

  @inline private[this] final def renderContent: Unit = {
    encoding = entity match {
      case Some(entity) if tooTinyToCareSize < entity.length || -1 == entity.length ⇒ request.transferEncoding
      case _ ⇒ None
    }
    entity match {
      case Some(entity) ⇒
        r(`Content-Type: `) + entity.contenttype + `\r\n` + ^
        encoding match {
          case Some(encoding) ⇒
            r(`Content-Encoding: `) + r(encoding.text) + `\r\n` + ^
            r(`Transfer-Encoding: chunked`) + `\r\n` + ^
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
          writebuffer.position(0)
        case _ ⇒
      }
      io ++ Done[Io, Boolean](keepalive)
    }
    entity match {
      case Some(entity: ByteBufferEntity) if entity.length <= writebuffer.remaining ⇒
        rb(entity.buffer) + ^
        releaseByteBuffer(entity.buffer)
        encode(entity)
      case Some(entity: ArrayEntity) if entity.length <= writebuffer.remaining ⇒
        r(entity.array) + ^
        encode(entity)
      case Some(_) ⇒
        io ++ Cont[Io, Boolean](null)
      case None ⇒
        io ++ Done[Io, Boolean](keepalive)
    }
  }

  private[this] final var encoding: Option[Encoder] = None

  private[this] final var markbuffer: ByteBuffer = null

  private[this] final implicit var bytebuffer: ByteBuffer = null

}

/**
 * The Response object.
 */
object Response {

  final def apply(request: Request, status: Status) = new Response(request, Version.`HTTP/1.1`, status, null, None)

  private final val `keep-alive` = "keep-alive".getBytes

  private final val `close` = "close".getBytes

  private final val `Connection: ` = "Connection: ".getBytes

  private final val `Content-Type: ` = "Content-Type: ".getBytes

  private final val `Content-Encoding: ` = "Content-Encoding: ".getBytes

  private final val `Content-Length: ` = "Content-Length: ".getBytes

  private final val `Transfer-Encoding: chunked` = "Transfer-Encoding: chunked".getBytes

  private final val `Server: ` = ("Server: plain " + config.version).getBytes

}

