package com.ibm

package plain

package http

import Message._

/**
 *
 */
trait Message {

  def headers: Headers

  def entity: Option[Entity]

  type Type <: Message

}

/**
 *
 */
object Message {

  type Headers = scala.collection.immutable.Map[String, String]

}