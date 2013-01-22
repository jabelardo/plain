package com.ibm

package plain

package http

import java.nio.ByteBuffer

import aio.Io
import aio.Renderable
import aio.Renderable._
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

  @inline final def render(implicit io: Io) = {
    println("response " + this)
    version + ` ` + status + `\r\n` + ^
    renderKeepAlive
    renderHeaders
    renderEntity
  }

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

  @inline final private def renderEntity(implicit io: Io) = entity match {
    case Some(entity: ArrayEntity) ⇒
      r("Content-Type: ") + entity.contenttype + `\r\n` + r("Content-Length: ") + r(entity.array.length.toString) + `\r\n` + `\r\n` + r(entity.array) + ^
    case _ ⇒
      `\r\n` + `\r\n` + ^
  }

  @inline final private def renderHeaders(implicit io: Io) = ()

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

}

/**
 * The Response object.
 */
object Response {

  def apply(request: Option[Request], status: Status) = new Response(request, Version.`HTTP/1.1`, status, Map.empty, None)

  def apply(status: Status): Response = apply(None, status)

}

