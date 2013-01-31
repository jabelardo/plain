package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import aio.{ Io, Input, Iteratee, ControlCompleted, Renderable, transfer, Iteratees, releaseByteBuffer }
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

  with Renderable

  with Handler[Long, Io] {

  type Type = Response

  private[this] implicit var ibuffer: ByteBuffer = null

  @inline final def completed(readwritten: Long, io: Io) = { println(readwritten); () }

  @inline final def failed(e: Throwable, io: Io) = throw e

  @inline override final def doRender(io: Io): Iteratee[Long, Boolean] = {
    import io._
    @inline def cont(total: Long)(input: Input[Long]): (Iteratee[Long, Boolean], Input[Long]) = input match {
      case Elem(written) ⇒
        println(written + " " + total + " " + (total + written) + " " + expected + " " + len)
        if (0 == written) {
          buffer.clear
          ibuffer = buffer
          version + ` ` + status + `\r\n` + ^
          renderKeepAlive(io)
          renderHeaders(io)
          entity match {
            case None ⇒
              buffer.flip
              (Done(keepalive), input)
            case Some(entity: ByteBufferEntity) if entity.length <= buffer.remaining ⇒
              r(entity.buffer) + ^
              buffer.flip
              (Done(keepalive), input)
            case _ ⇒
              buffer.flip
              len = buffer.remaining
              expected += len
              (Cont(cont(total + written)), input)
          }
        } else if (len == total + written) {
          renderEntity(io)
          (Cont(cont(total + written)), input)
        } else if (expected == total + written) {
          if (null != buf) {
            releaseByteBuffer(buffer)
            buffer = buf
          }
          (Done(keepalive), input)
        } else {
          (Cont(cont(total + written)), input)
        }
    }
    Cont(cont(0L))(Elem(0L))._1
  }

  final def render(implicit buffer: ByteBuffer) = ()

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  @inline final private def renderKeepAlive(io: Io): Unit = {
    val keepalive = status match {
      case _: Status.ServerError ⇒ false
      case _ ⇒ `Connection`(request.headers) match {
        case Some(value) if value.exists(_.equalsIgnoreCase("keep-alive")) ⇒ true
        case _ ⇒ false
      }
    }
    io ++ keepalive
    r("Connection: " + (if (keepalive) "keep-alive" else "close")) + `\r\n` + ^
  }

  @inline final private def renderHeaders(io: Io): Unit = {
    entity match {
      case Some(entity) ⇒
        r("Content-Type: ") + entity.contenttype + `\r\n` + r("Content-Length: ") + r(entity.length.toString) + `\r\n` + ^
        io ++ entity.length
      case _ ⇒
    }
    `\r\n` + ^
  }

  @inline final private def renderEntity(io: Io): Unit = entity match {
    case Some(entity: ByteBufferEntity) ⇒
      buf = io.buffer
      io.buffer = entity.buffer
    case Some(entity: ReadChannelEntity) ⇒
      transfer(entity.channel, io, this)
    case _ ⇒ throw new UnsupportedOperationException
  }

  private[this] var buf: ByteBuffer = null

  private[this] var len = 0L

}

/**
 * The Response object.
 */
object Response {

  def apply(request: Request, status: Status) = new Response(request, Version.`HTTP/1.1`, status, Map.empty, None)

}

