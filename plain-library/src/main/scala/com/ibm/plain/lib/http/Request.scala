package com.ibm.plain

package lib

package http

import Message._
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

  var entity: Option[Entity])

  extends Message

/**
 *
 */
object Request {

  type Path = scala.collection.immutable.Seq[String]

}