package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.util.continuations.{ reset, shift, suspendable }

import aio.{ Io, Input, Iteratee, RenderableRoot, ChannelTransfer, Iteratees, releaseByteBuffer }
import aio.Renderable._
import aio.Iteratee._
import aio.Input._
import Message._
import Entity._
import Status._
import Header.General.`Connection`

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

  final def renderBody(io: Io): Io @suspendable = null

  final def renderFooter(io: Io): Io = null

  final def renderHeader(io: Io): Io = {
    import io._
    buffer.clear
    implicit val _ = buffer
    version + ` ` + status + `\r\n` + ^
    val keepalive = status match {
      case _: Status.ServerError ⇒ false
      case _ ⇒ `Connection`(request.headers) match {
        case Some(value) if value.exists(_.equalsIgnoreCase("keep-alive")) ⇒ true
        case _ ⇒ false
      }
    }
    io ++ keepalive
    r("Connection: " + (if (keepalive) "keep-alive" else "close")) + `\r\n` + ^
    entity match {
      case Some(entity) ⇒
        r("Content-Type: ") + entity.contenttype + `\r\n` + r("Content-Length: ") + r(entity.length.toString) + `\r\n` + ^
        io ++ entity.length
      case _ ⇒
    }
    `\r\n` + ^
    entity match {
      case Some(entity: ByteBufferEntity) if entity.length <= buffer.remaining ⇒
        r(entity.buffer) + ^
        buffer.flip
        io ++ Done[Io, Boolean](keepalive)
      case Some(entity: ArrayEntity) if entity.length <= buffer.remaining ⇒
        r(entity.array) + ^
        buffer.flip
        io ++ Done[Io, Boolean](keepalive)
      case None ⇒
        buffer.flip
        io ++ Done[Io, Boolean](keepalive)
      case _ ⇒
        buffer.flip
        len = buffer.remaining
        expected += len
        io ++ Cont[Io, Boolean](null)
    }
  }

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  //  final private[this] def renderEntity(io: Io): Unit @suspendable = entity match {
  //    case Some(entity: ByteBufferEntity) ⇒
  //      buf = io.buffer
  //      io.buffer = entity.buffer
  //    case Some(entity: AsynchronousByteChannelEntity) ⇒
  //      ChannelTransfer(entity.channel, io.channel, io ++ entity.length).transfer
  //    case _ ⇒ throw new UnsupportedOperationException
  //  }
  //
  //  private[this] final var buf: ByteBuffer = null

  private[this] final var len = 0L

}

/**
 * The Response object.
 */
object Response {

  def apply(request: Request, status: Status) = new Response(request, Version.`HTTP/1.1`, status, Map.empty, None)

}

