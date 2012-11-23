package com.ibm

package plain

package http

import java.nio.ByteBuffer

import aio.Io
import aio.Renderable
import aio.Renderable._
import Message._
import Entity._

/**
 * The classic http response.
 */
final case class Response(

  version: Version,

  var status: Status,

  var headers: Headers,

  var entity: Option[Entity])

  extends Message

  with Renderable {

  type Type = Response

  @inline final def render(implicit io: Io) = {
    version + ` ` + status + `\r\n` + r("Connection: keep-alive") + `\r\n` + r("Content-Type: text/plain") + `\r\n` + r("Content-Length: 5") + `\r\n` + `\r\n` + r("PONG!") + ^
  }

  @inline final def ++(status: Status): Type = { this.status = status; this }

  @inline final def ++(headers: Headers): Type = { this.headers = headers; this }

}

/**
 * The Response object.
 */
object Response {

  def apply(status: Status) = new Response(Version.`HTTP/1.1`, status, Map.empty, None)

  def apply(resource: (Status, Option[Entity])) = new Response(Version.`HTTP/1.1`, resource._1, Map.empty, resource._2)

}

