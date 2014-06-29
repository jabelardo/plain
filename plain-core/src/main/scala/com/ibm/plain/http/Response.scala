package com.ibm

package plain

package http

import java.nio.ByteBuffer
import javax.servlet.http.Cookie

import scala.language.implicitConversions

import aio.{ Encoding, Exchange, ExchangeIo, ExchangeIteratee, Iteratee, OutMessage, releaseByteBuffer, tooTinyForEncodingSize }
import aio.Iteratee.{ Cont, Done }
import aio.Renderable._
import aio.conduit._
import text.`UTF-8`
import time.{ now, rfc1123 }
import Entity._
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

  var entity: Option[Entity])

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
    def rc(cookie: Cookie) = r((cookie.getName + "=" + cookie.getValue + (cookie.getPath match { case null ⇒ "" case path ⇒ "; Path=" + path }) + (if (cookie.isHttpOnly) "; HttpOnly" else "")).getBytes(`UTF-8`))
    r(`Set-Cookie: `) + rc(cookie) + `\r\n` + ^
  }

  @inline private[this] final def renderKeepAlive[A](exchange: Exchange[A]) = {
    if (!exchange.keepAlive) r(`Connection: close`) + ^
  }

  private[this] final def renderContentHeaders[A](exchange: Exchange[A]): Unit = {
    encoding(exchange)
    entity match {
      case Some(entity) ⇒
        r(`Content-Type: `) + entity.contenttype + `\r\n` + ^
        entity.transferencoding match {
          case Some(Encoding.`chunked`) ⇒
            r(`Transfer-Encoding: chunked`) + ^
          case Some(Encoding.`identity`) ⇒
            r(`Transfer-Encoding: identity`) + ^
          case _ ⇒
        }
        entity.contentencoding match {
          case Some(Encoding.`deflate`) ⇒
            r(`Content-Encoding: deflate`) + ^
          case Some(Encoding.`gzip`) ⇒
            r(`Content-Encoding: gzip`) + ^
          case Some(contentencoding) ⇒
            r(`Content-Encoding: `) + r(contentencoding.name.getBytes) + `\r\n` + ^
          case _ ⇒
            r(`Content-Length: `) + r(entity.length) + `\r\n` + `\r\n` + ^
        }
      case _ ⇒ r(`Content-Length: 0`) + ^
    }
  }

  @inline private[this] final def renderEntity[A](exchange: Exchange[A]): ExchangeIteratee[A] = {
    (entity match {
      case Some(ByteBufferEntity(buffer, _)) if buffer.remaining <= exchange.available ⇒
        rb(buffer) + ^
        releaseByteBuffer(buffer)
        true
      case Some(ArrayEntity(array, offset, length, _)) if length <= exchange.available ⇒
        r(array, offset, length.toInt) + ^
        true
      case Some(TransferEncodedEntity(entity, _)) ⇒
        val conduit: TerminatingConduit = entity match {
          case ArrayEntity(array, offset, length, _) ⇒
            ByteArrayConduit(array, offset, length.toInt)
          case ByteBufferEntity(bytebuffer, _) ⇒
            ByteArrayConduit(bytebuffer.array, 0, bytebuffer.array.length)
          case _ ⇒
            null
        }
        if (null != conduit) exchange.transferFrom(conduit)
        false
      case Some(_) ⇒ false
      case None ⇒ true
    }) match {
      case true ⇒
        exchange ++ done[A]
        done[A]
      case _ ⇒
        exchange ++ cont[A]
        cont[A]
    }
  }

  @inline private[this] final def encoding[A](exchange: Exchange[A]): Unit = entity match {
    case Some(e) ⇒
      if (e.encodable && (0 > e.length || tooTinyForEncodingSize < e.length)) {
        exchange.inMessage match {
          case request: Request ⇒
            request.acceptEncoding match {
              case Some(Encoding.`deflate`) ⇒
                exchange.setDestination(DeflateConduit(ChunkedConduit(exchange.socketChannel)))
                entity = Some(Entity.`deflate`(e))
              case Some(Encoding.`gzip`) ⇒
                exchange.setDestination(GzipConduit(ChunkedConduit(exchange.socketChannel)))
                entity = Some(Entity.`gzip`(e))
              case None if 0 > e.length ⇒
                exchange.setDestination(ChunkedConduit(exchange.socketChannel))
                entity = Some(Entity.`chunked`(e))
              case _ ⇒
            }
          case _ ⇒
        }
      } else if (0 > e.length) {
        exchange.setDestination(ChunkedConduit(exchange.socketChannel))
        entity = Some(Entity.`chunked`(e))
      }
    case _ ⇒
  }

}

/**
 * The Response object.
 */
object Response {

  final def apply(bytebuffer: ByteBuffer, status: Status) = new Response(bytebuffer, Version.`HTTP/1.1`, status, null, null, None)

  private final val `Connection: keep-alive` = "Connection: keep-alive\r\n".getBytes

  private final val `Connection: close` = "Connection: close\r\n".getBytes

  private final val `Set-Cookie: ` = "Set-Cookie: ".getBytes

  private final val `Content-Type: ` = "Content-Type: ".getBytes

  private final val `Content-Encoding: deflate` = "Content-Encoding: deflate\r\n".getBytes

  private final val `Content-Encoding: gzip` = "Content-Encoding: gzip\r\n".getBytes

  private final val `Content-Encoding: ` = "Content-Encoding: ".getBytes

  private final val `Content-Length: ` = "Content-Length: ".getBytes

  private final val `Content-Length: 0` = "Content-Length: 0\r\n\r\n".getBytes

  private final val `Transfer-Encoding: chunked` = "Transfer-Encoding: chunked\r\n".getBytes

  private final val `Transfer-Encoding: identity` = "Transfer-Encoding: identity\r\n".getBytes

  private final val `Server: server` = ("Server: plain.io " + config.version + "\r\n").getBytes

  private final val `Date: ` = "Date: ".getBytes

  private final val `Path=` = "Path=".getBytes

  private final val `HttpOnly` = "HttpOnly".getBytes

  private final def done[A]: Iteratee[ExchangeIo[A], _] = donevalue.asInstanceOf[Iteratee[ExchangeIo[A], _]]

  private final def cont[A]: Iteratee[ExchangeIo[A], _] = contvalue.asInstanceOf[Iteratee[ExchangeIo[A], _]]

  private[this] final val donevalue = Done[ExchangeIo[Null], Option[Nothing]](None)

  private[this] final val contvalue = Cont[ExchangeIo[Null], Null](null)

}

