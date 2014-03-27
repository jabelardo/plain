package com.ibm

package plain

package aio

import Exchange.ExchangeHandler

/**
 *
 */
trait AsynchronousProcessor {

  def process(exchange: Exchange, handler: ExchangeHandler)

}
