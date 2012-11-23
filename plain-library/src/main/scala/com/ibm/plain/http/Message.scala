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

  type Type <: Message

  final def ++(entity: Option[Entity]): Type = { this.entity = entity; this.asInstanceOf[Type] }

}

/**
 *
 */
object Message {

  type Headers = scala.collection.immutable.Map[String, String]

}