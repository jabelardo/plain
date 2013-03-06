package com.ibm

package plain

package http

import java.util.zip.Deflater

import aio.{ Compressor, DeflateCompressor, GZIPCompressor }
import Message.Headers
import Header.General.`Connection`
import Header.Request.`Accept-Encoding`

/**
 * The classic http request.
 */
final case class Request(

  method: Method,

  path: Request.Path,

  query: Option[String],

  version: Version,

  headers: Headers,

  var entity: Option[Entity])

  extends Message {

  type Type = Request

  final def keepalive = `Connection`(headers) match {
    case Some(value) if value.exists(_.equalsIgnoreCase("keep-alive")) ⇒ true
    case _ ⇒ false
  }

  final def transferEncoding: Option[Compressor] = `Accept-Encoding`(headers) match {
    case Some(value) if value.contains("deflate") ⇒ Some(DeflateCompressor(Deflater.BEST_SPEED))
    case Some(value) if value.contains("gzip") ⇒ Some(GZIPCompressor(Deflater.BEST_SPEED))
    case _ ⇒ None
  }

}

/**
 *
 */
object Request {

  type Path = Seq[String]

  type Variables = Map[String, String]

}