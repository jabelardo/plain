package com.ibm

package plain

package aio

/**
 *
 */
trait Message

/**
 *
 */
trait InMessage

  extends Message {

  val keepalive: Boolean

  def decoder: Option[Decoder]

}

/**
 *
 */
trait OutMessage

  extends Message {

  def renderHeader[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

  def encoder: Option[Encoder]

}

