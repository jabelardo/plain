package com.ibm.plain

package lib

package http

import scala.collection.mutable.{ HashMap, MutableList }

import org.apache.commons.codec.net.URLCodec

import aio._
import aio.Iteratee._
import aio.Iteratees._
import text.{ ASCII, UTF8 }
import Status.ServerError.`501`
import Header._
import Header.Entity._
import Entity._

/**
 * Consuming the input stream to produce a Request.
 */
class RequestIteratee()(implicit server: Server) {

  import RequestConstants._

  import server.settings.{ defaultCharacterSet, disableUrlDecoding }

  private[this] implicit final val ascii = ASCII

  private[this] final val codec = new URLCodec(defaultCharacterSet.toString)

  final val readRequestLine = {

    val readRequestUri: Iteratee[Io, (List[String], Option[String])] = {

      def readUriSegment(allowed: Set[Int]): Iteratee[Io, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (disableUrlDecoding) segment else codec.decode(segment)

      val readPath: Iteratee[Io, List[String]] = {

        @noinline def cont(segments: MutableList[String]): Iteratee[Io, List[String]] = peek(1) >>> {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readUriSegment(path)
            more ← cont(if (0 < segment.length) segments += segment else segments)
          } yield more
          case a ⇒ Done(segments.toList)
        }

        cont(MutableList.empty)
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
        case _ ⇒ throw `501`
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

  final val readHeaders: Iteratee[Io, Headers] = {

    val readHeader: Iteratee[Io, (String, String)] = {

      @noinline def cont(lines: String): Iteratee[Io, String] = peek(1) >>> {
        case " " | "\t" ⇒ for {
          _ ← drop(1)
          line ← takeUntil(`\r`)(defaultCharacterSet)
          _ ← drop(1)
          morelines ← cont(lines + line)
        } yield morelines
        case _ ⇒ Done(lines)
      }

      for {
        name ← takeWhile(token)(defaultCharacterSet)
        _ ← takeUntil(`:`)(defaultCharacterSet)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntil(`\r`)(defaultCharacterSet)
          _ ← drop(1)
          morelines ← cont(line)
        } yield morelines
      } yield (name.toLowerCase, value)
    }

    @noinline def cont(headers: HashMap[String, String]): Iteratee[Io, Headers] = peek(2) >>> {
      case "\r\n" ⇒ for {
        _ ← drop(2)
        done ← Done(headers.toMap)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        moreheaders ← cont(headers += header)
      } yield moreheaders
    }

    cont(HashMap.empty)
  }

  final def readEntity(headers: Headers): Iteratee[Io, Option[Entity]] = Done(
    `Content-Length`(headers) match {
      case Some(l) ⇒ Some(`Content-Type`(headers) match {
        case Some(t) ⇒ ContentEntity(l.toInt, t)
        case None ⇒ ContentEntity(l.toInt, "text/plain")
      })
      case _ ⇒ None
    })

  final val readRequest = for {
    (method, path, query, version) ← readRequestLine
    headers ← readHeaders
    entity ← readEntity(headers)
  } yield Request(method, path, query, version, headers, entity)

}
