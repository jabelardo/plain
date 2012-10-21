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

  entity: Option[Entity]) {

}

object Request {

  type Path = scala.collection.Seq[String]

  type Headers = scala.collection.Map[String, String]

}