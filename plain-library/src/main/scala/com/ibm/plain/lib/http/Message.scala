package com.ibm.plain

package lib

package http

import Message._

/**
 *
 */
trait Message {

  def headers: Headers

  var entity: Option[Entity]

  @inline final def ++(entity: Option[Entity]) = { this.entity = entity; this }

}

/**
 *
 */
object Message {

  type Headers = scala.collection.immutable.Map[String, String]

}