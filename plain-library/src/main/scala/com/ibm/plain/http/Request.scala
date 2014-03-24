package com.ibm

package plain

package http

import java.util.zip.Deflater

import aio.{ Encoder, InMessage }
import Message.Headers
import Header.General.`Connection`
import Header.Request.`Accept-Encoding`

/**
 * The classic http request.
 */
final case class Request(

  method: Method,

  path: Request.Path,

  private final val queryoption: Option[String],

  version: Version,

  headers: Headers,

  var entity: Option[Entity])

  extends Message

  with aio.InMessage {

  type Type = Request

  final val query = queryoption match {
    case None ⇒ None
    case Some(q) ⇒ Some(q.replace("_escaped_fragment_=", ""))
  }

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

  final def Get(path: String): Request = Request(Method.GET, path.split("/").toList.filter(0 < _.length), None, Version.`HTTP/1.1`, null, None)

}