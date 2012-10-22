package com.ibm.plain

package lib

package aio

import java.nio.channels.{ CompletionHandler ⇒ Handler }

trait AioHandler[A]

  extends Handler[A, Io] {

  def completed(a: A, io: Io)

  def failed(e: Throwable, io: Io)

}

