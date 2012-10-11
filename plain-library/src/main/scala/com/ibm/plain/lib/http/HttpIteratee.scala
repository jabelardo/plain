package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import aio.{ ByteBufferInput, Input, Iteratee, Done }
import aio.Iteratees.{ drop, peek, take, takeUntil, takeWhile }
import text.{ ASCII, UTF8 }

/**
 * Basic parsing constants.
 */
private object HttpConstants {

  val space = ' '.toByte
  val colon = ':'.toByte
  val at = '@'.toByte
  val slash = '/'.toByte
  val questionmark = '?'.toByte
  val percent = '%'.toByte
  val crlf = "\r\n".getBytes

  val alpha = (('a' to 'z') ++ ('A' to 'Z')).map(_.toByte).toSet
  val digit = ('0' to '9').map(_.toByte).toSet
  val hex = digit ++ (('a' to 'f') ++ ('A' to 'F')).map(_.toByte).toSet
  val other = Set('$', '=', '&', '!', '\'', '(', ')', ',', ';', '*', '+').map(_.toByte).toSet
  val allowedInPath = alpha ++ digit ++ other + colon + at
  val allowedInQuery = allowedInPath + slash + questionmark

  val ` ` = " "
  val `:` = ":"
  val `/` = "/"
  val `?` = "?"
  val `%` = "%"
  val `\r\n` = "\r\n"

}

/**
 * Consuming the input stream to produce a HttpRequest.
 */
private object HttpIteratees {

  import HttpConstants._

  private[this] implicit final val ascii = ASCII

  val readRequestLine = {

    val readRequestUri: Iteratee[ByteBufferInput, (Seq[String], Option[String])] = {

      def readUriSegment(allowed: Set[Byte]): Iteratee[ByteBufferInput, String] = {

        val readUrlEncoded: Iteratee[ByteBufferInput, String] = for {
          _ ← drop(1)
          encoded ← take(2)
        } yield text.unhexify(encoded)

        def readSegment(s: String, urlencoded: Boolean): Iteratee[ByteBufferInput, String] = {
          if (urlencoded) {
            for {
              c ← readUrlEncoded
              segment ← readUriSegment(allowed)
            } yield s + c + segment
          } else {
            Done(s)
          }
        }

        for {
          raw ← takeWhile(allowed.contains)(UTF8)
          isUrlEncoded ← peek(1) >> (`%`.equals)
          segment ← readSegment(raw, isUrlEncoded)
        } yield segment

      }

      //      if (isUrlEncoded)
      //            Done(raw) // readUrlEncoded >>= { c ⇒ readUriSegment(allowed) >= (raw + c + _) }
      //          else Done(raw)

      val readPath: Iteratee[ByteBufferInput, Seq[String]] = null

      val readQuery: Iteratee[ByteBufferInput, Option[String]] = peek(1) >>> {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readUriSegment(allowedInQuery)
        } yield Some(query)
        case _ ⇒ Done(None)
      }

      peek(1) >>> {
        case `/` ⇒ for {
          _ ← drop(1)
          path ← readPath
          query ← readQuery
        } yield (path, query)
        case _ ⇒ throw BadRequest("Invalid request URI.")
      }
    }

    for {
      method ← takeUntil(space)
      (uri, query) ← readRequestUri
      version ← takeUntil(crlf)
    } yield (HttpMethod(method), uri, query, version)
  }

  val readRequestHeaders: Iteratee[ByteBufferInput, Seq[HttpHeader]] = null

  def readRequestBody(headers: Seq[HttpHeader]): Iteratee[ByteBufferInput, HttpRequestBody] = null

  val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readRequestHeaders
    body ← readRequestBody(headers)
  } yield HttpRequest(method, path, query, version, headers, body)

}

object HttpTest extends App {

  import HttpIteratees._

  val input = Input.Elem(ByteBufferInput(ByteBuffer.wrap("GET /ping?w⇒h".getBytes("UTF8"))))

  readRequestLine(input) match {
    case e ⇒ println(e)
  }

}

