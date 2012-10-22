package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.util.continuations.{ shift, suspendable }

/**
 * An AioHandler processes an instance of E and produces an instance of A (hey, that's an Iteratee[E, A]!)
 */
trait AioHandler[E, A]

  extends Handler[A, Io] {

  def process(iter: Iteratee[Io, E], io: Io)

  def completed(a: A, io: Io)

  def failed(e: Throwable, io: Io)

  /**
   * called internally
   */
  private[aio] final def handle(iter: Iteratee[Io, E], io: Io): Io @suspendable = {
    import io._
    shift { k: Io.IoHandler ⇒ process(iter, io ++ k) }
  }

}

