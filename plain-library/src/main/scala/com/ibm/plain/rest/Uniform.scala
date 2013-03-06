package com.ibm

package plain

package rest

import aio.Io
import http.{ Request, Response, Processor ⇒ HttpProcessor }

/**
 * As postulated by Roy Fielding, but it works asynchronously.
 */
trait Uniform {

  def handle(context: Context)

  def completed(context: Context)

  def failed(e: Throwable, context: Context)

}

/**
 * A basic implementation for Uniform for correct exception handling.
 */
trait BaseUniform extends HttpProcessor with Uniform