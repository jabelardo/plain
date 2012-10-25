package com.ibm.plain

package lib

package http

import Request._

/**
 * The classic http request.
 */
case class Request(

  method: Method,

  path: Path,

  query: Option[String],

  version: Version,

  headers: Headers,

  var entity: Option[Entity]) {

  @inline final def ++(entity: Option[Entity]) = { this.entity = entity; this }

}

object Request {

  type Path = scala.collection.immutable.Seq[String]

  type Headers = scala.collection.immutable.Map[String, String]

}