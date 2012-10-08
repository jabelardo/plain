package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer
import aio.{ ByteBufferInput, Input }
import aio.Iteratees.{ drop, peek, takeUntil, take }

import text.{ ASCII, UTF8 }

/**
 * Supported http methods.
 */
sealed abstract class HttpMethod(val value: String, val idempotent: Boolean)

object HttpMethod {

  def apply(value: String): HttpMethod = value match {
    case "GET" ⇒ GET
    case "HEAD" ⇒ HEAD
    case "PUT" ⇒ PUT
    case "POST" ⇒ POST
    case "DELETE" ⇒ DELETE
    case m ⇒ throw BadRequest("Invalid method " + m)
  }

}

case object GET extends HttpMethod("GET", true)
case object HEAD extends HttpMethod("HEAD", true)
case object PUT extends HttpMethod("PUT", true)
case object POST extends HttpMethod("POST", false)
case object DELETE extends HttpMethod("DELETE", true)

/**
 *
 */
case class HttpHeader(name: String, value: String)

/**
 * The classic http request.
 */
case class HttpRequest(
  method: HttpMethod,
  path: Seq[String],
  query: Option[String],
  version: String,
  headers: Seq[HttpHeader])

/**
 * Basic parsing constants.
 */
object HttpConstants {

  val space = ' '.toByte
  val colon = ':'.toByte
  val slash = '/'.toByte
  val questionmark = '?'.toByte
  val percent = '%'.toByte
  val crlf = "\r\n".getBytes

  val SPACE = " "
  val COLON = ":"
  val SLASH = "/"
  val QUESTIONMARK = "?"
  val PERCENT = "%"
  val CRLF = "\r\n"

}

/**
 * Http errors.
 */
sealed abstract class HttpException(message: String) extends Exception(message)

case class BadRequest(message: String) extends HttpException(message)

/**
 * Consuming the input stream to produce a HttpRequest.
 */
object HttpIteratees {

  import HttpConstants._

  implicit val ascii = ASCII

  val readRequestLine1 = for {
    what ← take(181)
  } yield what

  val readRequestLine = for {
    method ← takeUntil(space)
    uri ← readUri
    version ← takeUntil(crlf)
    bloodyrest ← take(135)
  } yield (HttpMethod(method), uri, version, bloodyrest)

  val readUri = peek(1) >>= {
    case SLASH ⇒ for {
      _ ← drop(1)
      path ← takeUntil(space)(UTF8)
    } yield (Seq(path), "?")
    case _ ⇒ throw BadRequest("invalid uri")
  }

}

object HttpTest extends App {

  import HttpIteratees._

  val input = Input.Elem(ByteBufferInput(ByteBuffer.wrap("GET /ping?w⇒h".getBytes("UTF8"))))

  readRequestLine(input) match {
    case e ⇒ println(e)
  }

}

