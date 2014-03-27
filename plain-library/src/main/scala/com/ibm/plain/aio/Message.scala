package com.ibm

package plain

package aio

import Exchange.ExchangeIteratee

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

  def renderHeader(exchange: Exchange): ExchangeIteratee

  def renderBody(exchange: Exchange): ExchangeIteratee

  def renderFooter(exchange: Exchange): ExchangeIteratee

}

