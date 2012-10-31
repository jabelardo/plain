package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.util.continuations.{ shift, suspendable }

/**
 * An aio Processor processes an input of type E and produces a result of type A or an error.
 */
trait Processor[E, A <: Renderable]

  extends Handler[A, Io] {

  def process(io: Io)

  def completed(result: A, io: Io)

  def failed(e: Throwable, io: Io)

  private[aio] final def process_(io: Io): Io @suspendable = {
    import io._
    shift { k: Io.IoCont ⇒ try process(io ++ k) catch { case e: Throwable ⇒ failed(e, io) } }
  }

}
