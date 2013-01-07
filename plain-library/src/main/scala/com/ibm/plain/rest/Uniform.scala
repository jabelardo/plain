package com.ibm

package plain

package rest

import http.{ Request, Response }

/**
 * As postulated by Roy Fielding, but it works asynchronously.
 */
trait Uniform {

  def handle(request: Request, context: Context): Nothing

  def completed(response: Response, context: Context): Nothing

  def failed(e: Throwable, context: Context): Nothing

}

/**
 * A basic implementation for Uniform with a parent Uniform hidden in the context.
 */
trait BaseUniform

  extends Uniform {

  def completed(response: Response, context: Context): Nothing = context.parent.completed(response, context)

  def failed(e: Throwable, context: Context): Nothing = context.parent.failed(e, context)

}
