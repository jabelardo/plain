package com.ibm

package plain

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.util.continuations.{ shift, suspendable }

/**
 * An aio Processor processes an input of type E and produces a result of type A or an error.
 */
trait Processor[E, A]

  extends Handler[A, Io] {

  def process(io: Io): Nothing

  def completed(result: A, io: Io): Unit

  def failed(e: Throwable, io: Io): Unit

  private[aio] final def doProcess(io: Io): Io @suspendable = {
    import io._
    shift { k: Io.IoCont ⇒
      try process(io ++ k) catch {
        case ControlCompleted ⇒
        case e: Throwable ⇒ failed(e, io)
      }
    }
  }

}
