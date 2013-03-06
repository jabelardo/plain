package com.ibm

package plain

package http

import java.nio.ByteBuffer

import scala.util.continuations.suspendable

import Entity.{ ArrayEntity, AsynchronousByteChannelEntity, ByteBufferEntity }
import Message.Headers
import aio.{ ChannelTransfer, Encoder, Io }
import aio.{ RenderableRoot, releaseByteBuffer }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._

/**
 * The classic http response.
 */
final case class Response private (

  request: Request,

  version: Version,

  var status: Status,

  var headers: Headers,

  var entity: Option[Entity])

  extends Message

  with RenderableRoot {

  type Type = Response

  final def renderHeader(io: Io): Io = {
    import io._
    implicit val _ = io
    buffer.clear
    renderVersion
    renderKeepAlive
    renderContent
    renderEntity
    buffer.flip
    io
  }

  final def renderBody(io: Io): Io @suspendable = {
    import io._
    entity match {
      case Some(entity: AsynchronousByteChannelEntity) ⇒ encoder match {
        case Some(enc) ⇒
          ChannelTransfer(entity.channel, channel, io ++ Done[Io, Boolean](keepalive)).transfer(enc)
        case _ ⇒
          ChannelTransfer(entity.channel, channel, io ++ Done[Io, Boolean](keepalive)).transfer
      }
      case Some(entity: ByteBufferEntity) ⇒
        buf = buffer
        buffer = entity.buffer
        encoder match {
          case Some(enc) ⇒
            enc.encode(buffer)
            enc.finish(buffer)
          case _ ⇒
        }
        io ++ Done[Io, Boolean](keepalive)
      case Some(entity: ArrayEntity) ⇒
        buf = buffer
        buffer = ByteBuffer.wrap(entity.array)
        encoder match {
          case Some(enc) ⇒
            enc.encode(buffer)
            enc.finish(buffer)
          case _ ⇒
        }
        io ++ Done[Io, Boolean](keepalive)
      case _ ⇒ throw new UnsupportedOperationException
    }
  }

  final def renderFooter(io: Io): Io = {
    import io._
    if (null != buf) {
      releaseByteBuffer(buffer)
      buffer = buf
    }
    io
  }

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  @inline private[this] final def renderVersion(implicit io: Io) = {
    implicit val _ = io.buffer
    version + ` ` + status + `\r\n` + ^
  }

  @inline private[this] final def renderKeepAlive(implicit io: Io) = {
    implicit val _ = io.buffer
    val keepalive = !status.isInstanceOf[Status.ServerError] && null != request && request.keepalive
    io ++ keepalive
    r("Connection: " + (if (keepalive) "keep-alive" else "close")) + `\r\n` + ^
  }

  @inline private[this] final def renderContent(implicit io: Io): Unit = {
    import io._
    implicit val _ = buffer
    encoder = entity match {
      case Some(entity) if buffer.remaining < entity.length || -1 == entity.length ⇒ request.transferEncoding
      case _ ⇒ None
    }
    entity match {
      case Some(entity) ⇒
        r("Content-Type: ") + entity.contenttype + `\r\n` + ^
        encoder match {
          case Some(enc) ⇒
            r("Content-Encoding: ") + r(enc.name) + `\r\n` + ^
            r("Transfer-Encoding: chunked") + `\r\n` + ^
          case _ ⇒
            r("Content-Length: ") + r(entity.length.toString) + `\r\n` + ^
            `\r\n` + ^
        }
      case _ ⇒
    }
  }

  @inline private[this] final def renderEntity(implicit io: Io): Unit = {
    import io._
    implicit val _ = buffer
    entity match {
      case Some(entity: ByteBufferEntity) if entity.length <= buffer.remaining ⇒
        r(entity.buffer) + ^
        releaseByteBuffer(entity.buffer)
        io ++ Done[Io, Boolean](keepalive)
      case Some(entity: ArrayEntity) if entity.length <= buffer.remaining ⇒
        r(entity.array) + ^
        io ++ Done[Io, Boolean](keepalive)
      case None ⇒
        io ++ Done[Io, Boolean](keepalive)
      case Some(_) ⇒
        io ++ Cont[Io, Boolean](null)
    }
  }

  private[this] final var encoder: Option[Encoder] = None

  private[this] final var buf: ByteBuffer = null

}

/**
 * The Response object.
 */
object Response {

  def apply(request: Request, status: Status) = new Response(request, Version.`HTTP/1.1`, status, Map.empty, None)

}

