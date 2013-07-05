package com.ibm

package plain

package rest

import com.typesafe.config.Config

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
trait BaseUniform

  extends HttpProcessor

  with Uniform

/**
 * Mark any Uniform with this trait when it is safe to use an instance from different threads and repeatedly (that is it has no inner state).
 */
trait ReentrantUniform

