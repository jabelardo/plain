package com.ibm

package plain

package http

import Message.Headers

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
  
}

/**
 *
 */
object Request {

  type Path = Seq[String]

  type Variables = Map[String, String]

}