package com.ibm

package plain

package http

import org.apache.commons.codec.net.URLCodec

import aio._
import aio.Iteratee._
import aio.Iteratees._
import text.{ ASCII, UTF8 }
import Message.Headers
import Request.Path
import Status.ServerError.`501`
import Header.Entity.`Content-Length`
import Header.General.`Transfer-Encoding`
import Entity.{ ContentEntity, TransferEncodedEntity }
import ContentType.`text/plain`

/**
 * Consuming the input stream to produce a Request.
 */
final class RequestIteratee private ()(implicit server: Server) {

  import RequestConstants._

  import server.settings.{ defaultCharacterSet, disableUrlDecoding }

  private[this] implicit final val ascii = ASCII

  private[this] final val codec = new URLCodec(defaultCharacterSet.toString)

  private[this] final val readRequestLine = {

    val readRequestUri: Iteratee[Io, (Path, Option[String])] = {

      @inline def readUriSegment(allowed: Set[Int], nodecoding: Boolean): Iteratee[Io, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (nodecoding) segment else codec.decode(segment)

      val readPathSegment = readUriSegment(path, disableUrlDecoding)

      val readQuerySegment = readUriSegment(query, false)

      val readPath: Iteratee[Io, Path] = {

        @inline def cont(segments: List[String]): Iteratee[Io, Path] = peek(1) >>> {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readPathSegment
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(segments.reverse)
        }

        cont(Nil)
      }

      val readQuery: Iteratee[Io, Option[String]] = peek(1) >>> {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readQuerySegment
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

  private[this] final val readHeaders: Iteratee[Io, Headers] = {

    val readHeader: Iteratee[Io, (String, String)] = {

      @inline def cont(lines: String): Iteratee[Io, String] = peek(1) >>> {
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
        _ ← takeUntil(`:`)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntil(`\r`)(defaultCharacterSet)
          _ ← drop(1)
          morelines ← cont(line)
        } yield morelines
      } yield (name.toLowerCase, value)
    }

    @inline def cont(headers: List[(String, String)]): Iteratee[Io, Headers] = peek(1) >>> {
      case "\r" ⇒ for {
        _ ← drop(2)
        done ← Done(headers.toMap)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        moreheaders ← cont(header :: headers)
      } yield moreheaders
    }

    cont(Nil)
  }

  @inline private[this] final def readEntity(headers: Headers): Iteratee[Io, Option[Entity]] = Done(
    `Content-Length`(headers) match {
      case Some(length) ⇒ Some(ContentEntity(length))
      case None ⇒
        `Transfer-Encoding`(headers) match {
          case Some(value) ⇒ Some(TransferEncodedEntity(value))
          case None ⇒ None
        }
    })

  final val readRequest: Iteratee[Io, Request] = for {
    (method, path, query, version) ← readRequestLine
    headers ← readHeaders
    entity ← readEntity(headers)
  } yield Request(method, path, query, version, headers, entity)

}

object RequestIteratee {

  def apply(server: Server) = new RequestIteratee()(server)

}
