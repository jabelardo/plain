package com.ibm

package plain

package http

import java.nio.ByteBuffer
import javax.servlet.http.Cookie

import scala.language.implicitConversions

import aio.{ Encoder, Exchange, ExchangeIo, ExchangeIteratee, Iteratee, OutMessage, releaseByteBuffer, tooTinyToCareSize }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._
import aio.conduits.ByteArrayConduit
import text.`UTF-8`
import time.{ now, rfc1123 }
import Entity.{ ArrayEntity, AsynchronousByteChannelEntity, ByteBufferEntity }
import HttpMessage.Headers

/**
 * The classic http response.
 */
final case class Response(

  private final val bytebuffer: ByteBuffer,

  version: Version,

  var status: Status,

  var headers: Headers,

  var cookie: Cookie,

  var entity: Option[Entity],

  var encoder: Option[Encoder])

  extends OutMessage {

  import Response._

  implicit private[this] final val renderbuffer = bytebuffer

  @inline final def ++(status: Status) = { this.status = status; this }

  @inline final def ++(headers: Headers) = { this.headers = headers; this }

  @inline final def ++(entity: Option[Entity]) = { this.entity = entity; this }

  @inline final def ++(cookie: Cookie) = { this.cookie = cookie; this }

  /**
   * Render the response header and eventually the response body if it fits into the write buffer of the Exchange.
   */
  final def render[A](exchange: Exchange[A]): ExchangeIteratee[A] = {
    renderVersion
    renderMandatory
    renderHeaders
    renderCookie
    renderKeepAlive(exchange)
    renderContentHeaders(exchange)
    renderEntity(exchange)
  }

  /**
   * Privates.
   */

  @inline private[this] final def renderVersion = {
    version + ` ` + status + `\r\n` + ^
  }

  @inline private[this] final def renderMandatory = {
    r(`Server: server`) + r(`Date: `) + r(rfc1123) + `\r\n` + ^
  }

  @inline private[this] final def renderHeaders = if (null != headers) headers.foreach {
    case (name, value) ⇒ r(name.getBytes(`UTF-8`)) + `:` + ` ` + r(value.getBytes(`UTF-8`)) + `\r\n` + ^
  }

  @inline private[this] final def renderCookie = if (null != cookie) {
    r(`Set-Cookie: `) + rc(cookie) + `\r\n` + ^
  }

  @inline private[this] final def renderKeepAlive[A](exchange: Exchange[A]) = {
    if (!exchange.keepAlive) r(`Connection: close`) + ^
  }

  private[this] final def renderContentHeaders[A](exchange: Exchange[A]): Unit = {
    encoder = entity match {
      case Some(ArrayEntity(_, _, _, _)) ⇒ None
      case Some(ByteBufferEntity(_, _)) ⇒ None
      case Some(entity) if entity.contenttype.mimetype.encodable && (0 > entity.length || entity.length > tooTinyToCareSize) ⇒ exchange.inMessage match {
        case request: Request ⇒ request.acceptEncoding
        case _ ⇒ None
      }
      case _ ⇒ None
    }
    entity match {
      case Some(entity) ⇒
        r(`Content-Type: `) + entity.contenttype + `\r\n` + ^
        encoder match {
          case Some(encoder) ⇒
            r(`Content-Encoding: `) + r(encoder.text) + `\r\n` + ^
            r(`Transfer-Encoding: chunked`) + ^
          case _ ⇒
            r(`Content-Length: `) + r(entity.length) + `\r\n` + `\r\n` + ^
        }
      case _ ⇒ r(`Content-Length: `) + r(0) + `\r\n` + `\r\n` + ^
    }
  }

  @inline private[this] final def renderEntity[A](exchange: Exchange[A]): ExchangeIteratee[A] = entity match {
    case Some(entity: ByteBufferEntity) if entity.length <= exchange.available ⇒
      rb(entity.buffer) + ^
      releaseByteBuffer(entity.buffer)
      encode(exchange, entity)
    case Some(entity @ ArrayEntity(array, offset, length, _)) if length <= exchange.available ⇒
      r(array, offset, length.toInt) + ^
      encode(exchange, entity)
    case Some(ArrayEntity(array, offset, length, _)) ⇒
      exchange.transferFrom(ByteArrayConduit(array, offset, length.toInt))
      exchange ++ cont[A]
      cont[A]
    case Some(ByteBufferEntity(bytebuffer, _)) ⇒
      exchange.transferFrom(ByteArrayConduit(bytebuffer.array, 0, bytebuffer.array.length))
      exchange ++ cont[A]
      cont[A]
    case Some(_) ⇒
      exchange ++ cont[A]
      cont[A]
    case None ⇒
      exchange ++ done[A]
      done[A]
  }

  private[this] final def encode[A](exchange: Exchange[A], entity: Entity) = {
    encoder match {
      case Some(encoder) ⇒ exchange.encodeOnce(encoder, entity.length.toInt)
      case _ ⇒
    }
    exchange ++ done[A]
    done[A]
  }

  /**
   * What was this for?
   */
  @inline private[this] final def rc(cookie: Cookie) = r((cookie.getName + "=" + cookie.getValue + (cookie.getPath match { case null ⇒ "" case path ⇒ "; Path=" + path }) + (if (cookie.isHttpOnly) "; HttpOnly" else "")).getBytes(`UTF-8`))

}

/**
 * The Response object.
 */
object Response {

  final def apply(bytebuffer: ByteBuffer, status: Status) = new Response(bytebuffer, Version.`HTTP/1.1`, status, null, null, None, None)

  private final val `Connection: keep-alive` = "Connection: keep-alive\r\n".getBytes

  private final val `Connection: close` = "Connection: close\r\n".getBytes

  private final val `Set-Cookie: ` = "Set-Cookie: ".getBytes

  private final val `Content-Type: ` = "Content-Type: ".getBytes

  private final val `Content-Encoding: ` = "Content-Encoding: ".getBytes

  private final val `Content-Length: ` = "Content-Length: ".getBytes

  private final val `Transfer-Encoding: chunked` = "Transfer-Encoding: chunked\r\n".getBytes

  private final val `Server: server` = ("Server: plain.io " + config.version + "\r\n").getBytes

  private final val `Date: ` = "Date: ".getBytes

  private final val `Path=` = "Path=".getBytes

  private final val `HttpOnly` = "HttpOnly".getBytes

  private final def done[A]: Iteratee[ExchangeIo[A], _] = donevalue.asInstanceOf[Iteratee[ExchangeIo[A], _]]

  private final def cont[A]: Iteratee[ExchangeIo[A], _] = contvalue.asInstanceOf[Iteratee[ExchangeIo[A], _]]

  private[this] final val donevalue = Done[ExchangeIo[Null], Option[Nothing]](None)

  private[this] final val contvalue = Cont[ExchangeIo[Null], Null](null)

}

