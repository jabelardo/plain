package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import aio._
import aio.Encoding._
import aio.Iteratee._
import aio.Iteratees._
import aio.Input._
import text.{ `US-ASCII`, utf8Codec }
import HttpMessage.Headers
import Request.Path
import Status.ClientError.`400`
import Header.Entity.{ `Content-Encoding`, `Content-Length`, `Content-Type` }
import Header.General.`Transfer-Encoding`
import Header.Request.`Expect`
import Version.`HTTP/1.1`
import ContentType.fromMimeType
import Entity.{ ArrayEntity, ContentEntity, TransferEncodedEntity }
import MimeType.{ `text/plain`, `application/octet-stream` }
import Server.ServerConfiguration
import logging.Logger

/**
 * Consuming the input stream to produce a Request.
 */
object RequestIteratee

  extends Logger {

  import RequestConstants._

  private[this] final def readRequestLine[A](settings: ServerConfiguration, server: Server): Iteratee[ExchangeIo[A], RequestLine] = {

    val characterset = settings.defaultCharacterSet

    val readMethod: Iteratee[ExchangeIo[A], Method] = peek flatMap {
      case `G` ⇒ for (_ ← drop(4)) yield Method.GET
      case `H` ⇒ for (_ ← drop(5)) yield Method.HEAD
      case `D` ⇒ for (_ ← drop(7)) yield Method.DELETE
      case `C` ⇒ for (_ ← drop(8)) yield Method.CONNECT
      case `O` ⇒ for (_ ← drop(8)) yield Method.OPTIONS
      case `T` ⇒ for (_ ← drop(6)) yield Method.TRACE
      case _ ⇒ for (name ← takeUntil(` `, characterset, false)) yield Method(name)
    }

    val readRequestUri: Iteratee[ExchangeIo[A], (Path, Option[String])] = {

      def readUriSegment(allowed: Set[Int], nodecoding: Boolean): Iteratee[ExchangeIo[A], String] = for {
        segment ← takeWhile(allowed, characterset, false)
      } yield if (nodecoding) segment else utf8Codec.decode(segment)

      val readPathSegment = readUriSegment(path, settings.disableUrlDecoding)

      val readQuerySegment = readUriSegment(query, false)

      val readPath: Iteratee[ExchangeIo[A], Path] = {

        def cont(segments: List[String]): Iteratee[ExchangeIo[A], Path] = peek flatMap {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readPathSegment
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(if (1 < segments.length) segments.reverse else segments)
        }

        cont(Nil)
      }

      val readQuery: Iteratee[ExchangeIo[A], Option[String]] = peek flatMap {
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
      method ← readMethod
      uri ← readRequestUri
      _ ← takeWhile(whitespace, characterset, false)
      version ← takeUntilCrLf(characterset, false)
    } yield RequestLine(method, uri._1, uri._2, Version(version, server))
  }

  private[this] final def readHeaders[A](characterset: Charset): Iteratee[ExchangeIo[A], Headers] = {

    val readHeader: Iteratee[ExchangeIo[A], (String, String)] = {

      def cont(lines: String): Iteratee[ExchangeIo[A], String] = peek flatMap {
        case ` ` | `\t` ⇒ for {
          _ ← drop(1)
          line ← takeUntilCrLf(characterset, false)
          morelines ← cont(lines + line)
        } yield morelines
        case _ ⇒ Done(lines)
      }

      for {
        name ← takeWhile(token, characterset, true)
        _ ← takeUntil(`:`, characterset, false)
        _ ← takeWhile(whitespace, characterset, false)
        value ← for {
          line ← takeUntilCrLf(characterset, false)
          morelines ← cont(line)
        } yield morelines
      } yield (name, value)
    }

    @inline def cont(headers: List[(String, String)]): Iteratee[ExchangeIo[A], Headers] = peek flatMap {
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

  private[this] final def readEntity[A](

    headers: Headers,

    query: Option[String],

    settings: ServerConfiguration): Iteratee[ExchangeIo[A], Option[Entity]] = {

    def contenttype: ContentType = `Content-Type`(headers) match {
      case Some(contenttype) ⇒ contenttype
      case _ ⇒ `application/octet-stream`
    }

    def need100continue = `Expect`(headers) match {
      case Some(expect) if "100-continue" == expect.toLowerCase ⇒ true
      case _ ⇒ false
    }

    if (ignoreEntityEncoding) {
      Done(None)
    } else {
      `Transfer-Encoding`(headers) match {
        case Some(transferencoding) ⇒
          for (_ ← continue(Long.MaxValue, continuebuffer.duplicate))
            yield Some(TransferEncodedEntity(transferencoding, ContentEntity(contenttype, `Content-Encoding`(headers) match {
            case Some(value) ⇒ Encoding(value)
            case _ ⇒ None
          })))
        case None ⇒ `Content-Length`(headers) match {
          case Some(length) ⇒
            if (need100continue) {
              for (_ ← continue(length, continuebuffer.duplicate)) yield Some(ContentEntity(contenttype, length))
            } else if (length < tooTinyForEncodingSize) {
              for (array ← takeBytes(length.toInt)) yield Some(ArrayEntity(array, contenttype))
            } else {
              Done(Some(ContentEntity(contenttype, length)))
            }
          case None ⇒ query match {
            case Some(query) ⇒ Done(Some(ArrayEntity(query.getBytes(defaultCharacterSet), `text/plain`)))
            case None ⇒ Done(None)
          }
        }
      }
    }
  }

  final def readRequest[A](server: Server): Iteratee[ExchangeIo[A], Request] = {
    val settings = server.getSettings
    for {
      line ← readRequestLine(settings, server)
      headers ← readHeaders(settings.defaultCharacterSet)
      entity ← readEntity(headers, line.query, settings)
    } yield Request(line.method, line.path, line.query, line.version, headers, entity)
  }

  @inline private[this] final case class RequestLine(method: Method, path: Path, query: Option[String], version: Version)

  @inline private[this] final val continuebuffer = {
    val response = "HTTP/1.1 100 Continue\r\n\r\n".getBytes
    val buffer = ByteBuffer.allocateDirect(response.length)
    buffer.put(response)
    buffer.flip
    buffer
  }

}
