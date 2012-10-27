package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import aio.Renderable
import aio.Renderable._
import Message._

/**
 * The classic http response.
 */
case class Response(

  version: Version,

  var status: Status,

  var headers: Headers,

  var entity: Option[Entity])

  extends Message

  with Renderable {

  @inline final def render(implicit buffer: ByteBuffer) = {
    version + ` ` + status + `\r\n` + r("Connection: keep-alive") + `\r\n` + r("Content-Type: text/plain") + `\r\n` + r("Content-Length: 5") + `\r\n` + `\r\n` + r("PONG!") + ^
  }

  @inline final def ++(status: Status) = { this.status = status; this }

  @inline final def ++(headers: Headers) = { this.headers = headers; this }

}

/**
 * The Response object.
 */
object Response {

  def apply(status: Status) = new Response(Version.`HTTP/1.1`, status, Map.empty, None)

  def apply(resource: (Status, Option[Entity])) = new Response(Version.`HTTP/1.1`, resource._1, Map.empty, resource._2)

}

