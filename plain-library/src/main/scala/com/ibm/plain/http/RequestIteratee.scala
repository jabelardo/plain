package com.ibm

package plain

package http

import aio._
import aio.Iteratee._
import aio.Iteratees._
import aio.Input._
import text._
import Message.Headers
import Request.Path
import Status.ClientError.`400`
import Header.Entity.{ `Content-Length`, `Content-Type` }
import Header.General.`Transfer-Encoding`
import Version.`HTTP/1.1`
import Entity.{ ArrayEntity, ContentEntity, TransferEncodedEntity }
import MimeType.`text/plain`

/**
 * Consuming the input stream to produce a Request.
 */
final class RequestIteratee private ()(implicit server: Server) {

  import RequestConstants._

  import server.settings.{ defaultCharacterSet, disableUrlDecoding, maxEntityBufferSize }

  private[this] implicit final val ascii = `US-ASCII`

  private[this] final val readRequestLine = {

    val readRequestUri: Iteratee[Io, (Path, Option[String])] = {

      def readUriSegment(allowed: Set[Int], nodecoding: Boolean): Iteratee[Io, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (nodecoding) segment else utf8codec.decode(segment)

      val readPathSegment = readUriSegment(path, disableUrlDecoding)

      val readQuerySegment = readUriSegment(query, false)

      val readPath: Iteratee[Io, Path] = {

        def cont(segments: List[String]): Iteratee[Io, Path] = peek flatMap {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readPathSegment
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(if (1 < segments.length) segments.reverse else segments)
        }

        cont(Nil)
      }

      val readQuery: Iteratee[Io, Option[String]] = peek flatMap {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readQuerySegment
        } yield Some(query)
        case _ ⇒ Done(None)
      }

      peek flatMap {
        case `/` ⇒ for {
          path ← readPath
          query ← readQuery
        } yield (path, query)
        case _ ⇒ throw `400`
      }
    }

    for {
      method ← peek flatMap {
        case `G` ⇒ for (_ ← drop(4)) yield Method.GET
        case `H` ⇒ for (_ ← drop(5)) yield Method.HEAD
        case `D` ⇒ for (_ ← drop(7)) yield Method.DELETE
        case `C` ⇒ for (_ ← drop(8)) yield Method.CONNECT
        case `O` ⇒ for (_ ← drop(8)) yield Method.OPTIONS
        case `T` ⇒ for (_ ← drop(6)) yield Method.TRACE
        case _ ⇒ for (name ← takeUntil(` `)) yield Method(name)
      }
      pathquery ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntilCrLf
    } yield (method, pathquery._1, pathquery._2, Version(version))
  }

  private[this] final def readHeaders: Iteratee[Io, Headers] = {

    val readHeader: Iteratee[Io, (String, String)] = {

      def cont(lines: String): Iteratee[Io, String] = peek flatMap {
        case ` ` | `\t` ⇒ for {
          _ ← drop(1)
          line ← takeUntilCrLf(defaultCharacterSet)
          morelines ← cont(lines + line)
        } yield morelines
        case _ ⇒ Done(lines)
      }

      for {
        name ← takeWhile(token)(defaultCharacterSet)
        _ ← takeUntil(`:`)
        _ ← takeWhile(whitespace)
        value ← for {
          line ← takeUntilCrLf(defaultCharacterSet)
          morelines ← cont(line)
        } yield morelines
      } yield (name.toLowerCase, value)
    }

    def cont(headers: List[(String, String)]): Iteratee[Io, Headers] = peek flatMap {
      case `\r` ⇒ for {
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

  private[this] final def readEntity(headers: Headers, query: Option[String]): Iteratee[Io, Option[Entity]] =
    `Content-Type`(headers) match {
      case Some(contenttype) ⇒ `Transfer-Encoding`(headers) match {
        case Some(value) ⇒ Done(Some(TransferEncodedEntity(value, contenttype)))
        case notransfer ⇒ `Content-Length`(headers) match {
          case Some(length) if length <= maxEntityBufferSize ⇒
            (for (array ← takeBytes(length.toInt)) yield Some(ArrayEntity(array, contenttype)))
          case Some(length) ⇒ Done(Some(ContentEntity(contenttype, length)))
          case nolength ⇒ Done(None)
        }
      }
      case _ ⇒ query match {
        case Some(s) ⇒ Done(Some(ArrayEntity(s.getBytes(defaultCharacterSet), `text/plain`)))
        case _ ⇒ Done(None)
      }
    }

  final def readRequest: Iteratee[Io, Request] = for {
    mpqv ← readRequestLine
    headers ← readHeaders
    entity ← readEntity(headers, mpqv._3)
  } yield Request(mpqv._1, mpqv._2, mpqv._3, mpqv._4, headers, entity)

}

object RequestIteratee {

  final def apply(server: Server) = new RequestIteratee()(server)

}
