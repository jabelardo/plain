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

  def keepalive: Boolean

  def decoder: Option[Decoder]

}

/**
 *
 */
trait OutMessage

  extends Message {

  def render[A](exchange: Exchange[A]): ExchangeIteratee[A]

  def encoder: Option[Encoder]

}

