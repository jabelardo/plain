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

}

/**
 *
 */
trait OutMessage

  extends Message {

  def renderMessageHeader[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

  def renderMessageBody[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

  def renderMessageFooter[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

}

