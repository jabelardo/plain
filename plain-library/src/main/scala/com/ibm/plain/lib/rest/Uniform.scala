package com.ibm.plain

package lib

package rest

import aio.AioDone
import http.{ Request, Response }

/**
 * As postulated by Roy Fielding, but it works asynchronously.
 */
trait Uniform {

  def handle(request: Request, context: Context): Nothing

  def completed(response: Response, context: Context)

  def failed(e: Throwable, context: Context)

}

/**
 * A basic implementation for Uniform with a parent Uniform hidden in the context.
 */
trait BaseUniform

  extends Uniform {

  def completed(response: Response, context: Context) = context.parent.completed(response, context)

  def failed(e: Throwable, context: Context) = context.parent.failed(e, context)

  protected[this] final def handled = throw AioDone

}
