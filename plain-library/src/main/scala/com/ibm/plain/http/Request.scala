package com.ibm

package plain

package http

import java.util.zip.Deflater

import aio.Encoder
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
    case Some(value) if value.exists(_.equalsIgnoreCase("close")) ⇒ false
    case _ ⇒ true
  }

  final def transferEncoding: Option[Encoder] = `Accept-Encoding`(headers) match {
    case Some(value) if value.contains("deflate") ⇒ Some(DeflateEncoder(Deflater.BEST_SPEED))
    case Some(value) if value.contains("gzip") ⇒ Some(GZIPEncoder(Deflater.BEST_SPEED))
    case _ ⇒ None
  }

}

/**
 *
 */
object Request {

  type Path = List[String]

  type Variables = scala.collection.Map[String, String]

}