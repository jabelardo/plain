package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import scala.collection.immutable.{ BitSet, NumericRange, Range ⇒ SRange }

import org.apache.commons.codec.net.URLCodec

import com.ibm.plain.lib.aio.Iteratee

import HttpConstants.codec
import aio.{ Io, Done, Input, Iteratee }
import aio.Iteratees.{ drop, peek, take, takeUntil, takeWhile }
import logging.HasLogger
import text.{ ASCII, UTF8 }
import time.infoNanos

/**
 * Basic parsing constants.
 */
private object HttpConstants {

  final val ` ` = " ".getBytes
  final val `\t` = "\t".getBytes
  final val `:` = ":".getBytes
  final val `/` = "/"
  final val `?` = "?"
  final val `\r\n` = "\r\n".getBytes
  final val crlf = "\r\n"
  final val del = 127.toByte

  final val char = b(0 to 127)
  final val lower = b('a' to 'z')
  final val upper = b('A' to 'Z')
  final val alpha = lower | upper
  final val digit = b('0' to '9')
  final val hex = digit | b('a' to 'f') | b('A' to 'F')
  final val control = b(0 to 31) + del
  final val whitespace = b(' ', '\t')
  final val separators = whitespace | b('(', ')', '[', ']', '<', '>', '@', ',', ';', ':', '\\', '\"', '/', '?', '=', '{', '}')
  final val text = char -- control ++ whitespace
  final val token = char -- control -- separators
  final val gendelimiters = b(':', '/', '?', '#', '[', ']', '@')
  final val subdelimiters = b('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
  final val reserved = gendelimiters | subdelimiters
  final val unreserved = alpha | digit | b('-', '.', '_', '~')
  final val path = unreserved | subdelimiters | b(':', '@', '%')
  final val query = path | b('/', '?', '#')

  final lazy val codec = new URLCodec(defaultCharacterSet.toString)

  @inline private[this] def b(in: Int*): Set[Int] = BitSet(in: _*)
  @inline private[this] def b(in: SRange.Inclusive): Set[Int] = BitSet(in: _*)
  @inline private[this] def b(in: NumericRange.Inclusive[Char]): Set[Int] = BitSet(in.map(_.toInt): _*)

}

/**
 * Consuming the input stream to produce a HttpRequest.
 */
private object HttpIteratee {

  import HttpConstants._

  private[this] implicit final val ascii = ASCII

  final val readToken = for {
    token ← takeWhile(token)(defaultCharacterSet)
  } yield token

  final val readText = for {
    text ← takeWhile(text)(defaultCharacterSet)
  } yield text

  final val readRequestLine = {

    val readRequestUri: Iteratee[Io, (List[String], Option[String])] = {

      def readUriSegment(allowed: Set[Int]): Iteratee[Io, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (disableUrlDecoding) p(segment) else p(codec.decode(segment))

      val readPath: Iteratee[Io, List[String]] = {

        @noinline def cont(segments: List[String]): Iteratee[Io, List[String]] = peek(1) >>> {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readUriSegment(path)
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield p(more)
          case a ⇒ Done(segments.reverse)
        }

        cont(List.empty)
      }

      val readQuery: Iteratee[Io, Option[String]] = peek(1) >>> {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readUriSegment(query)
        } yield p(Some(query))
        case _ ⇒ Done(None)
      }

      peek(1) >>> {
        case `/` ⇒ for {
          path ← readPath
          query ← readQuery
        } yield p((path, query))
        case _ ⇒ throw BadRequest("Invalid request URI.")
      }
    }

    for {
      method ← takeUntil(` `)
      (uri, query) ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntil(`\r\n`)
    } yield p((HttpMethod(method), uri, query, HttpVersion(version)))

  }

  final val readRequestHeaders: Iteratee[Io, List[HttpHeader]] = {

    val readHeader: Iteratee[Io, HttpHeader] = {

      @noinline def cont(lines: String): Iteratee[Io, String] = peek(1) >>> {
        case " " | "\t" ⇒ for {
          _ ← drop(1)
          line ← takeUntil(`\r\n`)
          more ← cont(lines + line)
        } yield more
        case _ ⇒ Done(lines)
      }

      for {
        name ← readToken
        _ ← takeUntil(`:`)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntil(`\r\n`)
          morelines ← cont(line)
        } yield morelines
      } yield p(HttpHeader(name, value))

    }

    @noinline def cont(headers: List[HttpHeader]): Iteratee[Io, List[HttpHeader]] = peek(2) >>> {
      case "\r\n" ⇒ for {
        _ ← takeUntil(`\r\n`)
        done ← Done(headers.reverse)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        moreheaders ← cont(header :: headers)
      } yield moreheaders
    }

    cont(List.empty)
  }

  final def readRequestBody(headers: List[HttpHeader]): Iteratee[Io, HttpRequestBody] = {
    Done(NoneRequestBody)
  }

  final val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readRequestHeaders
    body ← readRequestBody(headers)
  } yield p(HttpRequest(method, path, query, version, headers, body))

  private[this] final def p[A](a: A): A = { if (false) println("result " + a); a }

}

object HttpTest extends App with HasLogger {

  import HttpIteratee._

  def apply = try {

    println(HttpConstants.token)
    println(HttpConstants.hex)

    // val req = "GET /a/b//////c/d/e//XYZ/abc/def/?thisquery HTTP/1.1\r\n".getBytes(UTF8)
    // val req = "GET /a/b//////c/%32%33d/e//XYZ/%c3%84_%c3%96_%c3%9c_%c3%a4_%c3%b6_%c3%bc_%c3%9f/%E9%BA%B5%E5%8C%85/?this%20query%21%E4%BA%8C%E4%B8%8D%E4%BA%8C%E7%9A%84%E4%BA%8C/%21 HTTP/1.1\r\n".getBytes(UTF8)

    val req = "GET /a/b//////c/%32%33d/e//XYZ/%c3%84_%c3%96_%c3%9c_%c3%a4_%c3%b6_%c3%bc_%c3%9f/%E9%BA%B5%E5%8C%85/?this%20query%21%E4%BA%8C%E4%B8%8D%E4%BA%8C%E7%9A%84%E4%BA%8C/%21#this_is_fragment HTTP/1.1\r\nHost: localhost:7500\r\nAccept: */*\r\nAccept-Encoding: gzip, deflate; g=1.0\r\nUser-Agent: JoeDog/1.00 [en] (X11; I; Siege 2.72)\r\n more user agent.\r\nConnection: keep-alive\r\n\r\n".getBytes(UTF8)

    for (_ ← 1 to 1) infoNanos(try {
      val input = Input.Elem(Io.empty ++ ByteBuffer.wrap(req))
      readRequest(input) match {
        case (Done(r), _) ⇒ // println(r)
        case e ⇒ println(e)
      }
    } catch { case e: Throwable ⇒ throw e })
  } catch {
    case e: Throwable ⇒ e.printStackTrace
  }

}

