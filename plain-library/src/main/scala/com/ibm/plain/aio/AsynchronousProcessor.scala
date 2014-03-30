package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

/**
 *
 */
trait AsynchronousProcessor[A] {

  def process(exchange: Exchange[A], handler: Handler[Integer, Exchange[A]])

}
