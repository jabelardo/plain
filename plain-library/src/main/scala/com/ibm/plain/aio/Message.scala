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

  def renderHeader[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

  def renderBody[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

  def renderFooter[A](exchange: Exchange[A]): Iteratee[Exchange[A], _]

}

