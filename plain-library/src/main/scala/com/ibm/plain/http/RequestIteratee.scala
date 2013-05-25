package com.ibm

package plain

package http

import org.apache.commons.codec.net.URLCodec

import scala.collection.mutable.OpenHashMap

import aio._
import aio.Iteratee._
import aio.Iteratees._
import aio.Input._
import text._
import Message.Headers
import Request.Path
import Status.ServerError.`501`
import Header.Entity.{ `Content-Length`, `Content-Type` }
import Header.General.`Transfer-Encoding`
import Entity.{ ArrayEntity, ContentEntity, TransferEncodedEntity }
import MimeType.`text/plain`

/**
 * Consuming the input stream to produce a Request.
 */
final class RequestIteratee private ()(implicit server: Server) {

  import RequestConstants._

  import server.settings.{ defaultCharacterSet, disableUrlDecoding, maxEntityBufferSize }

  private[this] implicit final val ascii = `US-ASCII`

  private[this] final val codec = new URLCodec(`UTF-8`.toString)

  private[this] final val readRequestLine = {

    val readRequestUri: Iteratee[Io, (Path, Option[String])] = {

      @inline def readUriSegment(allowed: Set[Int], nodecoding: Boolean): Iteratee[Io, String] = for {
        segment ← takeWhile(allowed)(defaultCharacterSet)
      } yield if (nodecoding) segment else codec.decode(segment)

      val readPathSegment = readUriSegment(path, disableUrlDecoding)

      val readQuerySegment = readUriSegment(query, false)

      val readPath: Iteratee[Io, Path] = {

        @inline def cont(segments: List[String]): Iteratee[Io, Path] = peek(1) flatMap {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readPathSegment
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(if (1 < segments.length) segments.reverse else segments)
        }

        cont(Nil)
      }

      val readQuery: Iteratee[Io, Option[String]] = peek(1) flatMap {
        case `?` ⇒ for {
          _ ← drop(1)
          query ← readQuerySegment
        } yield Some(query)
        case _ ⇒ Done(None)
      }

      peek(1) flatMap {
        case `/` ⇒ for {
          path ← readPath
          query ← readQuery
        } yield (path, query)
        case _ ⇒ throw `501`
      }
    }

    for {
      method ← takeUntil(` `)
      pathquery ← readRequestUri
      _ ← takeWhile(whitespace)
      version ← takeUntil(`\r`)
      _ ← drop(1)
    } yield (Method(method), pathquery._1, pathquery._2, Version(version))
  }

  private[this] final val readHeaders: Iteratee[Io, Headers] = {

    val readHeader: Iteratee[Io, (String, String)] = {

      @inline def cont(lines: String): Iteratee[Io, String] = peek(1) flatMap {
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

    def cont(headers: OpenHashMap[String, String]): Iteratee[Io, Headers] = peek(1) flatMap {
      case "\r" ⇒ for {
        _ ← drop(2)
        done ← Done(headers)
      } yield done
      case _ ⇒ for {
        header ← readHeader
        moreheaders ← cont(headers += header)
      } yield moreheaders
    }

    cont(new OpenHashMap[String, String])
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
      case nocontent ⇒ query match {
        case Some(s) ⇒ Done(Some(ArrayEntity(s.getBytes(defaultCharacterSet), `text/plain`)))
        case _ ⇒ Done(None)
      }

    }

  final val readRequest: Iteratee[Io, Request] = for {
    mpqv ← readRequestLine
    headers ← readHeaders
    entity ← readEntity(headers, mpqv._3)
  } yield Request(mpqv._1, mpqv._2, mpqv._3, mpqv._4, headers, entity)

}

object RequestIteratee {

  final def apply(server: Server) = new RequestIteratee()(server)

}
