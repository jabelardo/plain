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
 * Indicates that there is only one instance of this resource which is also thread-safe.
 */
trait StaticResource

  extends Uniform {

  def init(config: Config) = ()

}

/**
 * A basic implementation for Uniform for correct exception handling.
 */
trait BaseResource

  extends HttpProcessor

  with Uniform

