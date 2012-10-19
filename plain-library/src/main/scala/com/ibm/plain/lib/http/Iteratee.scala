package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import scala.collection.immutable.{ BitSet, NumericRange, Range ⇒ SRange }

import org.apache.commons.codec.net.URLCodec

import aio.Iteratee._
import aio.Iteratees._
import aio._
import logging.HasLogger
import text.{ ASCII, UTF8 }
import Status.ServerError.`501`

/**
 * Consuming the input stream to produce a Request.
 */
class RequestIteratee()(implicit server: Server) {

  import RequestConstants._

  import server.settings.{ defaultCharacterSet, disableUrlDecoding }

  private[this] implicit final val ascii = ASCII

  private[this] final lazy val codec = new URLCodec(defaultCharacterSet.toString)

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
        case _ ⇒ throw new `501`
      }
    }

    for {
      method ← takeUntil(` `)
      (uri, query) ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntil(`\r`)
      _ ← drop(1)
    } yield (Method(method), uri, query, Version(version))

  }

  final val readRequestHeaders: Iteratee[Io, List[Header]] = {

    val readHeader: Iteratee[Io, Header] = {

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
      } yield Header(name, value)

    }

    @noinline def cont(headers: List[Header]): Iteratee[Io, List[Header]] = peek(2) >>> {
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

  final def readRequestBody(headers: List[Header]): Iteratee[Io, Option[RequestBody]] = {
    Done(None)
  }

  final val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readRequestHeaders
    body ← readRequestBody(headers)
  } yield Request(method, path, query, version, headers, body)

}

