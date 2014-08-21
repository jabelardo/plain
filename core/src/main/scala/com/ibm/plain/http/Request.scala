package com.ibm

package plain

package http

import aio.{ Encoding, InMessage }
import Header.General.{ `Connection`, `Transfer-Encoding` }
import Header.Entity.{ `Content-Encoding` }
import Header.Request.{ `Accept-Encoding` }

/**
 *
 */
object HttpMessage {

  type Headers = scala.collection.Map[String, String]

}

/**
 * The classic http request.
 */
final case class Request(

  method: Method,

  path: Request.Path,

  private final val queryoption: Option[String],

  version: Version,

  headers: HttpMessage.Headers,

  entity: Option[Entity])

  extends InMessage {

  type Type = Request

  final def query = queryoption match {
    case None ⇒ None
    case Some(q) ⇒ Some(q.replace("_escaped_fragment_=", ""))
  }

  final def keepalive = `Connection`(headers) match {
    case Some(value) if value.exists(_.equalsIgnoreCase("close")) ⇒ false
    case _ ⇒ true
  }

  final def acceptEncoding: Option[Encoding] = {
    `Accept-Encoding`(headers) match {
      case Some(accept) ⇒ if (accept.contains("deflate")) Some(Encoding.`deflate`) else if (accept.contains("gzip")) Some(Encoding.`gzip`) else None
      case _ ⇒ None
    }
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