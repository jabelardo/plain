package com.ibm.plain

package lib

package http

import Message._
import Request._

/**
 * The classic http request.
 */
final case class Request(

  method: Method,

  path: Path,

  query: Option[String],

  version: Version,

  headers: Headers,

  var entity: Option[Entity])

  extends Message {

  @inline final def ++(entity: Option[Entity]): this.type = { this.entity = entity; this }

}

/**
 *
 */
object Request {

  type Path = Seq[String]

  type Variables = Map[String, String]

}