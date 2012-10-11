package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import org.apache.commons.codec.net.URLCodec

import aio.{ ByteBufferInput, Input, Iteratee, Done }
import aio.Iteratees._
import text.{ ASCII, UTF8 }
import logging._
import time._

/**
 * Basic parsing constants.
 */
private object HttpConstants {

  final val ` ` = ' '.toByte
  final val `\t` = '\t'.toByte
  final val `:` = ':'.toByte
  final val `/` = "/"
  final val `?` = "?"
  final val `\r\n` = "\r\n".getBytes
  final val crlf = "\r\n"
  final val del = 127.toByte

  final val octet = b(0 to 255)
  final val char = b(0 to 127)
  final val lower = b('a' to 'z')
  final val upper = b('A' to 'Z')
  final val alpha = lower | upper
  final val digit = b('0' to '9')
  final val hex = digit | b('a' to 'f') | b('A' to 'F')
  final val control = b(0 to 31) + del
  final val whitespace = b(` `, `\t`)
  final val separators = whitespace | b('(', ')', '[', ']', '<', '>', '@', ',', ';', ':', '\\', '\"', '/', '?', '=', '{', '}')
  final val text = octet -- control ++ whitespace
  final val token = char -- control -- separators
  final val gendelimiters = b(':', '/', '?', '#', '[', ']', '@')
  final val subdelimiters = b('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
  final val reserved = gendelimiters | subdelimiters
  final val unreserved = alpha | digit | b('-', '.', '_', '~')
  final val path = unreserved | subdelimiters | b(':', '@', '%')
  final val query = path | b('/', '?', '#')

  final val codec = new URLCodec(UTF8.toString)

  @inline private[this] def b(in: Int*): Set[Byte] = in.map(_.toByte).toSet
  @inline private[this] def b(in: scala.collection.immutable.Range.Inclusive): Set[Byte] = in.map(_.toByte).toSet
  @inline private[this] def b(in: scala.collection.immutable.NumericRange.Inclusive[Char]): Set[Byte] = in.map(_.toByte).toSet

}

/**
 * Consuming the input stream to produce a HttpRequest.
 */
private object HttpIteratees {

  import HttpConstants._

  private[this] implicit final val ascii = ASCII

  val readToken = for {
    token ← takeWhile(token)(defaultCharacterSet)
  } yield token

  val readText = for {
    text ← takeWhile(text)(defaultCharacterSet)
  } yield text

  val readRequestLine = {

    val readRequestUri: Iteratee[ByteBufferInput, (Seq[String], Option[String])] = {

      def readUriSegment(allowed: Set[Byte]): Iteratee[ByteBufferInput, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (disableUrlDecoding) segment else codec.decode(segment)

      val readPath: Iteratee[ByteBufferInput, Seq[String]] = {

        @noinline def cont(segments: Seq[String]): Iteratee[ByteBufferInput, Seq[String]] = peek(1) >>> {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readUriSegment(path)
            more ← cont(if (0 < segment.length) segments :+ segment else segments)
          } yield more
          case _ ⇒ Done(segments)
        }

        cont(Vector.empty)
      }

      val readQuery: Iteratee[ByteBufferInput, Option[String]] = peek(1) >>> {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readUriSegment(query)
        } yield Some(query)
        case _ ⇒ Done(None)
      }

      peek(1) >>> {
        case `/` ⇒ for {
          path ← readPath
          query ← readQuery
        } yield (path, query)
        case _ ⇒ throw BadRequest("Invalid request URI.")
      }
    }

    for {
      method ← takeUntil(` `)
      (uri, query) ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntil(`\r\n`)
    } yield (HttpMethod(method), uri, query, HttpVersion(version))
  }

  val readRequestHeaders: Iteratee[ByteBufferInput, Seq[HttpHeader]] = {

    val readHeader: Iteratee[ByteBufferInput, HttpHeader] = {

      @noinline def readMultipleLines(lines: String): Iteratee[ByteBufferInput, String] = peek(1) >>> {
        case " " | "\t" ⇒ for {
          _ ← drop(1)
          line ← takeUntil(`\r\n`)
          more ← readMultipleLines(lines + line)
        } yield more
        case _ ⇒ Done(lines)
      }

      for {
        name ← readToken
        _ ← takeUntil(`:`)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntil(`\r\n`)
          lines ← readMultipleLines(line)
        } yield lines
      } yield HttpHeader(name, value)
    }

    @noinline def cont(headers: Seq[HttpHeader]): Iteratee[ByteBufferInput, Seq[HttpHeader]] = peek(2) >>> {
      case "\r\n" ⇒ for {
        _ ← takeUntil(`\r\n`)
        done ← Done(headers)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        more ← cont(headers :+ header)
      } yield more
    }

    cont(Vector.empty)
  }

  def readRequestBody(headers: Seq[HttpHeader]): Iteratee[ByteBufferInput, HttpRequestBody] = {
    headers.foreach(_ match {
      case length @ `Content-Length`(_) ⇒
        val bytes = for {
          body ← take(length.intValue)
        } yield body.getBytes
        return Done(BytesRequestBody(bytes.result))
      case _ ⇒ ()
    })
    Done(NoneRequestBody)
  }

  val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readRequestHeaders
    body ← readRequestBody(headers)
  } yield HttpRequest(method, path, query, version, headers, body)

}

object HttpTest extends App with HasLogger {

  import HttpIteratees._

  def apply = try {

    HttpHeader.test

    // val req = "GET /a/b//////c/d/e//XYZ/abc/def/?thisquery HTTP/1.1\r\n".getBytes(UTF8)
    // val req = "GET /a/b//////c/%32%33d/e//XYZ/%c3%84_%c3%96_%c3%9c_%c3%a4_%c3%b6_%c3%bc_%c3%9f/%E9%BA%B5%E5%8C%85/?this%20query%21%E4%BA%8C%E4%B8%8D%E4%BA%8C%E7%9A%84%E4%BA%8C/%21 HTTP/1.1\r\n".getBytes(UTF8)

    val req = "GET /a/b//////c/%32%33d/e//XYZ/%c3%84_%c3%96_%c3%9c_%c3%a4_%c3%b6_%c3%bc_%c3%9f/%E9%BA%B5%E5%8C%85/?this%20query%21%E4%BA%8C%E4%B8%8D%E4%BA%8C%E7%9A%84%E4%BA%8C/%21#this_is_fragment HTTP/1.1\r\nHost: localhost:7500\r\nAccept: */*\r\nAccept-Encoding: gzip, deflate; g=1.0\r\nUser-Agent: JoeDog/1.00 [en] (X11; I; Siege 2.72)\r\n more user agent.\r\nConnection: keep-alive\r\n\r\n".getBytes(UTF8)

    for (_ ← 1 to 10) infoNanoNanos(try {
      val input = Input.Elem(ByteBufferInput(ByteBuffer.wrap(req)))
      readRequest(input) match {
        case (Done(r), _) ⇒ println(r)
        case e ⇒ println(e)
      }
    } catch { case e: Throwable ⇒ throw e })
  } catch {
    case e: Throwable ⇒ e.printStackTrace
  }

}

