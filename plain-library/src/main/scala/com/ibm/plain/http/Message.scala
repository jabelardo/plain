package com.ibm

package plain

package http

import Message._

/**
 *
 */
trait Message {

  def headers: Headers

  var entity: Option[Entity]

}

/**
 *
 */
object Message {

  type Headers = scala.collection.immutable.Map[String, String]

}