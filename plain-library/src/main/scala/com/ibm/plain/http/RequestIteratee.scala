package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import aio._
import aio.Iteratee._
import aio.Iteratees._
import aio.Input._
import text.{ `US-ASCII`, utf8Codec }
import Message.Headers
import Request.Path
import Status.ClientError.`400`
import Header.Entity.{ `Content-Length`, `Content-Type` }
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

  private[this] final def readRequestLine[A](settings: ServerConfiguration, server: Server): Iteratee[Exchange[A], (Method, Path, Option[String], Version)] = {

    val characterset = settings.defaultCharacterSet

    val readMethod: Iteratee[Exchange[A], Method] = peek flatMap {
      case `G` ⇒ for (_ ← drop(4)) yield Method.GET
      case `H` ⇒ for (_ ← drop(5)) yield Method.HEAD
      case `D` ⇒ for (_ ← drop(7)) yield Method.DELETE
      case `C` ⇒ for (_ ← drop(8)) yield Method.CONNECT
      case `O` ⇒ for (_ ← drop(8)) yield Method.OPTIONS
      case `T` ⇒ for (_ ← drop(6)) yield Method.TRACE
      case _ ⇒ for (name ← takeUntil(` `, characterset, false)) yield Method(name)
    }

    val readRequestUri: Iteratee[Exchange[A], (Path, Option[String])] = {

      def readUriSegment(allowed: Set[Int], nodecoding: Boolean): Iteratee[Exchange[A], String] = for {
        segment ← takeWhile(allowed, characterset, false)
      } yield if (nodecoding) segment else utf8Codec.decode(segment)

      val readPathSegment = readUriSegment(path, settings.disableUrlDecoding)

      val readQuerySegment = readUriSegment(query, false)

      val readPath: Iteratee[Exchange[A], Path] = {

        def cont(segments: List[String]): Iteratee[Exchange[A], Path] = peek flatMap {
          case `/` ⇒ for {
            _ ← drop(1)
            segment ← readPathSegment
            more ← cont(if (0 < segment.length) segment :: segments else segments)
          } yield more
          case a ⇒ Done(if (1 < segments.length) segments.reverse else segments)
        }

        cont(Nil)
      }

      val readQuery: Iteratee[Exchange[A], Option[String]] = peek flatMap {
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
    } yield (method, uri._1, uri._2, Version(version, server))
  }

  private[this] final def readHeaders[A](characterset: Charset): Iteratee[Exchange[A], Headers] = {

    val readHeader: Iteratee[Exchange[A], (String, String)] = {

      def cont(lines: String): Iteratee[Exchange[A], String] = peek flatMap {
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

    @inline def cont(headers: List[(String, String)]): Iteratee[Exchange[A], Headers] = peek flatMap {
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

  private[this] final def readEntity[A](headers: Headers, query: Option[String], settings: ServerConfiguration): Iteratee[Exchange[A], Option[Entity]] = {

    def `100-continue`[A](n: Int): Iteratee[Exchange[A], Array[Byte]] = {

      object ContinueWriteHandler

        extends Handler[Integer, Exchange[A]] {

        @inline final def completed(processed: Integer, exchange: Exchange[A]) = () // trace("Replied to 100-continue.")

        @inline final def failed(e: Throwable, exchange: Exchange[A]) = ()

        @inline final def response = responsebuffer.duplicate

        private[this] final val responsebuffer = {
          val response = "HTTP/1.1 100 Continue\r\n\r\n".getBytes
          val buffer = ByteBuffer.allocateDirect(response.length)
          buffer.put(response)
          buffer.flip
          buffer
        }
      }

      def cont(taken: Exchange[A])(input: Input[Exchange[A]]): (Iteratee[Exchange[A], Array[Byte]], Input[Exchange[A]]) = {
        input match {
          case Elem(more) ⇒
            val in = if (null == taken) more else taken ++ more
            if (0 == in.length) {
              in.socketChannel.write(ContinueWriteHandler.response, in, ContinueWriteHandler)
              (Cont(cont(in)), Empty)
            } else {
              if (in.length < n) {
                (Cont(cont(in)), Empty)
              } else {
                (Done(in.take(n).consume), Elem(in))
              }
            }
          case Failure(e) ⇒ (Error(e), input)
          case _ ⇒ (Error(null), input)
        }
      }

      Cont(cont(null) _)
    }

    `Content-Type`(headers) match {
      case Some(contenttype) ⇒ `Transfer-Encoding`(headers) match {
        case Some(value) ⇒ Done(Some(TransferEncodedEntity(value, contenttype)))
        case None ⇒ `Content-Length`(headers) match {
          case Some(length) if length <= settings.maxEntityBufferSize ⇒ for (array ← `100-continue`(length.toInt)) yield Some(ArrayEntity(array, 0, length, contenttype))
          case Some(length) ⇒ Done(Some(ContentEntity(contenttype, length)))
          case None ⇒ Done(None)
        }
      }
      case None ⇒ `Content-Length`(headers) match {
        case Some(length) if length <= settings.maxEntityBufferSize ⇒
          for (array ← `100-continue`(length.toInt)) yield Some(ArrayEntity(array, 0, length, `application/octet-stream`))
        case Some(length) ⇒
          for { _ ← `100-continue`(0); done ← Done(Some(ContentEntity(`application/octet-stream`, length))) } yield done
        case None ⇒ query match {
          case Some(query) ⇒ Done(Some(ArrayEntity(query.getBytes(defaultCharacterSet), `text/plain`)))
          case None ⇒ Done(None)
        }
      }
    }
  }

  final def readRequest[A](server: Server): Iteratee[Exchange[A], Request] = {
    val settings = server.getSettings
    for {
      mpqv ← readRequestLine(settings, server)
      headers ← readHeaders(settings.defaultCharacterSet)
      entity ← readEntity(headers, mpqv._3, settings)
    } yield Request(mpqv._1, mpqv._2, mpqv._3, mpqv._4, headers, entity, None)
  }

}
