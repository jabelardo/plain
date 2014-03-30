package com.ibm

package plain

package http

import java.nio.ByteBuffer
import javax.servlet.http.Cookie

import scala.language.implicitConversions

import aio.{ AsynchronousTransfer, Encoder, Exchange, ExchangeIteratee, OutMessage, releaseByteBuffer, tooTinyToCareSize }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._
import text.`UTF-8`
import time.{ now, rfc1123 }
import Entity.{ ArrayEntity, AsynchronousByteChannelEntity, ByteBufferEntity }
import Message.Headers

/**
 * The classic http response.
 */
final case class Response(

  private final val bytebuffer: ByteBuffer,

  version: Version,

  var status: Status,

  var headers: Headers,

  var cookie: Cookie,

  var entity: Option[Entity])

  extends OutMessage {

  import Response._

  implicit private[this] final val renderbuffer = bytebuffer

  @inline final def ++(status: Status) = { this.status = status; this }

  @inline final def ++(headers: Headers) = { this.headers = headers; this }

  @inline final def ++(entity: Option[Entity]) = { this.entity = entity; this }

  @inline final def ++(cookie: Cookie) = { this.cookie = cookie; this }

  /**
   * Called first and always.
   */
  final def renderMessageHeader[A](exchange: Exchange[A]): ExchangeIteratee[A] = if (null == responsebuffer) {
    renderVersion
    renderMandatory
    renderHeaders
    renderCookie
    renderKeepAlive(exchange)
    renderContentHeaders(exchange)
    renderEntity(exchange)
    val len = renderbuffer.position
    val limit = renderbuffer.limit
    renderbuffer.position(0)
    renderbuffer.limit(len)
    responsebuffer = ByteBuffer.allocate(len)
    responsebuffer.put(renderbuffer)
    responsebuffer.flip
    renderbuffer.position(len)
    renderbuffer.limit(limit)
    done[A]
  } else {
    bytebuffer.put(responsebuffer.duplicate)
    exchange ++ done[A]
    done[A]
  }

  /**
   * Called second if first return Cont.
   */
  final def renderMessageBody[A](exchange: Exchange[A]): ExchangeIteratee[A] = {
    entity match {
      case Some(entity: AsynchronousByteChannelEntity) ⇒
        exchange ++ AsynchronousTransfer(entity.channel, exchange.socketChannel, encoder)
        exchange ++ cont[A]
        cont[A]
      case Some(entity: ByteBufferEntity) ⇒
        exchange.swap(entity.buffer)
        encode(exchange, entity)
      case Some(entity @ ArrayEntity(array, offset, length, _)) ⇒
        exchange.swap(ByteBuffer.wrap(array, offset, length.toInt))
        encode(exchange, entity)
      case _ ⇒ unsupported
    }
  }

  /**
   * Called last if and only if second was called and return a Cont.
   */
  final def renderMessageFooter[A](exchange: Exchange[A]): ExchangeIteratee[A] = {
    exchange.swap(null)
    exchange ++ done[A]
    done[A]
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

  @inline private[this] final def renderContentHeaders[A](exchange: Exchange[A]): Unit = {
    encoder = entity match {
      case Some(entity) if entity.contenttype.mimetype.encodable && entity.length > tooTinyToCareSize ⇒ exchange.inMessage match {
        case request: Request ⇒ request.transferEncoding
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
      case _ ⇒ `\r\n` + ^
    }
  }

  @inline private[this] final def renderEntity[A](exchange: Exchange[A]): ExchangeIteratee[A] = {

    entity match {
      case Some(entity: ByteBufferEntity) if entity.length <= exchange.available ⇒
        rb(entity.buffer) + ^
        releaseByteBuffer(entity.buffer)
        encode(exchange, entity)
      case Some(entity @ ArrayEntity(array, offset, length, _)) if length <= exchange.available ⇒
        r(array, offset, length.toInt) + ^
        encode(exchange, entity)
      case Some(_) ⇒
        exchange ++ cont[A]
        cont[A]
      case None ⇒
        exchange ++ done[A]
        done[A]
    }
  }

  @inline private[this] final def encode[A](exchange: Exchange[A], entity: Entity) = {
    encoder match {
      case Some(encoder) ⇒ exchange.encode(encoder, entity.length.toInt)
      case _ ⇒
    }
    exchange ++ done[A]
    done[A]
  }

  /**
   * What was this for?
   */
  @inline private[this] final def rc(cookie: Cookie) = r((cookie.getName + "=" + cookie.getValue + (cookie.getPath match { case null ⇒ "" case path ⇒ "; Path=" + path }) + (if (cookie.isHttpOnly) "; HttpOnly" else "")).getBytes(`UTF-8`))

  private[this] final var encoder: Option[Encoder] = None

}

/**
 * The Response object.
 */
object Response {

  final def apply(bytebuffer: ByteBuffer, status: Status) = new Response(bytebuffer, Version.`HTTP/1.1`, status, null, null, None)

  final def apply(bytebuffer: ByteBuffer, status: Status, headers: Headers) = new Response(bytebuffer, Version.`HTTP/1.1`, status, headers, null, None)

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

  private final def done[A]: ExchangeIteratee[A] = Done[Exchange[A], Option[Nothing]](None)

  private final def cont[A]: ExchangeIteratee[A] = Cont[Exchange[A], Null](null)

  private final var responsebuffer: ByteBuffer = null

}

