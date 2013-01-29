package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import aio.{ Io, Input, Iteratee, ControlCompleted, Renderable, transfer, Iteratees }
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

  request: Option[Request],

  version: Version,

  var status: Status,

  var headers: Headers,

  var entity: Option[Entity])

  extends Message

  with Renderable {

  type Type = Response

  @inline override final def doRender(implicit io: Io): Iteratee[Long, Boolean] = {
    def cont(total: Long)(input: Input[Long]): (Iteratee[Long, Boolean], Input[Long]) = {
      input match {
        case Elem(written) if 0 == written ⇒
          io.buffer.clear
          version + ` ` + status + `\r\n` + ^
          renderKeepAlive
          renderHeaders
          io.buffer.flip
          length = io.buffer.remaining
          io.expected += length
          (Cont(cont(total + written)), input)
        case Elem(written) if length == total + written ⇒
          renderEntity
          (Cont(cont(total + written)), input)
        case Elem(written) if io.expected == total + written ⇒
          if (null != buffer) io.buffer = buffer
          (Done(io.keepalive), input)
        case Elem(written) ⇒
          (Cont(cont(total + written)), input)
      }
    }
    Cont(cont(0L))
  }

  final def render(implicit io: Io) = ()

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  @inline final private def renderEntity(implicit io: Io): Unit = entity match {
    case Some(arrayentity: ArrayEntity) ⇒
      buffer = io.buffer
      io.buffer = ByteBuffer.wrap(arrayentity.array)
    case Some(entity: ReadChannelEntity) ⇒ transfer(entity.channel, io, null)
    case _ ⇒ throw new UnsupportedOperationException
  }

  @inline final private def renderHeaders(implicit io: Io) = entity match {
    case Some(entity) ⇒
      r("Content-Type: ") + entity.contenttype + `\r\n` + r("Content-Length: ") + r(entity.length.toString) + `\r\n` + `\r\n` + ^
      io ++ entity.length
    case _ ⇒
  }

  @inline final private def renderKeepAlive(implicit io: Io) = {
    val keepalive = request match {
      case Some(req) ⇒ `Connection`(req.headers) match {
        case Some(value) if value.exists(_.equalsIgnoreCase("keep-alive")) ⇒ status match {
          case _: Status.ServerError ⇒ false
          case _ ⇒ true
        }
        case _ ⇒ false
      }
      case _ ⇒ false
    }
    io ++ keepalive
    r("Connection: " + (if (keepalive) "keep-alive" else "close")) + `\r\n` + ^
  }

  private[this] var buffer: ByteBuffer = null

  private[this] var length = 0L

}

/**
 * The Response object.
 */
object Response {

  def apply(request: Option[Request], status: Status) = new Response(request, Version.`HTTP/1.1`, status, Map.empty, None)

  def apply(status: Status): Response = apply(None, status)

}

