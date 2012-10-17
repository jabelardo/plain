package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import scala.collection.immutable.{ BitSet, NumericRange, Range ⇒ SRange }

import org.apache.commons.codec.net.URLCodec

import com.ibm.plain.lib.aio.Iteratee

import aio.Iteratee._
import aio.Iteratees._
import aio._
import logging.HasLogger
import text.{ ASCII, UTF8 }
import time.infoNanos

/**
 * Basic parsing constants.
 */
private object HttpConstants {

  final val ` ` = ' '.toByte
  final val `\t` = '\t'.toByte
  final val `:` = ':'.toByte
  final val `\r` = '\r'.toByte
  final val del = 127.toByte

  final val `/` = "/"
  final val `?` = "?"
  final val cr = "\r"
  final val lf = "\n"

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
      } yield if (disableUrlDecoding) segment else codec.decode(segment)

      val readPath: Iteratee[Io, List[String]] = {

        @noinline def cont(segments: List[String]): Iteratee[Io, List[String]] = peek(1) >>> {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readUriSegment(path)
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(segments.reverse)
        }

        cont(List.empty)
      }

      val readQuery: Iteratee[Io, Option[String]] = peek(1) >>> {
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
        case _ ⇒ throw HttpException.BadRequest("Invalid request URI.")
      }
    }

    for {
      method ← takeUntil(` `)
      (uri, query) ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntil(`\r`)
      _ ← drop(1)
    } yield (HttpMethod(method), uri, query, HttpVersion(version))

  }

  final val readRequestHeaders: Iteratee[Io, List[HttpHeader]] = {

    val readHeader: Iteratee[Io, HttpHeader] = {

      @noinline def cont(lines: String): Iteratee[Io, String] = peek(1) >>> {
        case " " | "\t" ⇒ for {
          _ ← drop(1)
          line ← takeUntil(`\r`)(defaultCharacterSet)
          _ ← drop(1)
          more ← cont(lines + line)
        } yield more
        case _ ⇒ Done(lines)
      }

      for {
        name ← readToken
        _ ← takeUntil(`:`)(defaultCharacterSet)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntil(`\r`)(defaultCharacterSet)
          _ ← drop(1)
          morelines ← cont(line)
        } yield morelines
      } yield HttpHeader(name, value)

    }

    @noinline def cont(headers: List[HttpHeader]): Iteratee[Io, List[HttpHeader]] = peek(2) >>> {
      case "\r\n" ⇒ for {
        _ ← drop(2)
        done ← Done(headers.reverse)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        moreheaders ← cont(header :: headers)
      } yield moreheaders
    }

    cont(List.empty)
  }

  final def readRequestBody(headers: List[HttpHeader]): Iteratee[Io, Option[HttpRequestBody]] = {
    Done(None)
  }

  final val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readRequestHeaders
    body ← readRequestBody(headers)
  } yield HttpRequest(method, path, query, version, headers, body)

  /**
   * simple testing
   */
  final val readRequestTest = for {
    all ← take(100)
  } yield all

  // @inline private[this] final def p[A](a: A): A = a // { println("result [" + a + "]"); a }

}

