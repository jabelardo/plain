package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

/**
 *
 */
trait Processor[A] {

  def process(exchange: Exchange[A], handler: Handler[Integer, Exchange[A]])

}
