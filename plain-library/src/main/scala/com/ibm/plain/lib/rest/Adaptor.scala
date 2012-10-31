package com.ibm.plain

package lib

package rest

import java.nio.channels.{ CompletionHandler ⇒ Handler }

import aio.Io
import http.Response
import http.Status.Success

final class Adaptor private (

  uniform: Uniform,

  context: Context)

  extends Handler[Long, Io] {

  @inline final def completed(readwritten: Long, io: Io) = uniform.completed(Response(Success.`200`), context ++ io)

  @inline final def failed(e: Throwable, io: Io) = uniform.failed(e, context ++ io)

}

object Adaptor {

  def apply(uniform: Uniform, context: Context) = new Adaptor(uniform, context)

}