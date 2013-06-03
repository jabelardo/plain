package com.ibm

package plain

package rest

import com.typesafe.config.Config

import aio.Io
import http.{ Request, Response }
import http.Request.{ Path, Variables }
import Resource.MethodBody

/**
 * A wrapper to hold shared context among Uniforms.
 */
final class Context private (

  val io: Io,

  var config: Config,

  var variables: Variables,

  var remainder: Path,

  var request: Request,

  var response: Response,

  var throwable: Throwable,

  var methodbody: MethodBody) {

  @inline final def ++(config: Config) = { this.config = config; this }

  @inline final def ++(variables: Variables) = { this.variables = variables; this }

  @inline final def ++(remainder: Path) = { this.remainder = remainder; this }

  @inline final def ++(request: Request) = { this.request = request; this }

  @inline final def ++(response: Response) = { this.response = response; this }

  @inline final def ++(throwable: Throwable) = { this.throwable = throwable; this }

  @inline final def ++(methodbody: MethodBody) = { this.methodbody = methodbody; this }

}

/**
 *
 */
object Context {

  @inline def apply(io: Io) = new Context(io, null, null, null, null, null, null, null)

}
