package com.ibm

package plain

package rest

import com.typesafe.config.Config

import http.{ Request, Response }
import http.Request.{ Path, Variables }
import Resource.MethodBody

/**
 * A wrapper to hold shared context among Uniforms.
 */
final class Context private (

  var config: Config,

  var variables: Variables,

  var remainder: Path,

  var request: Request,

  var response: Response) {

  @inline final def ++(config: Config) = { this.config = config; this }

  @inline final def ++(variables: Variables) = { this.variables = variables; this }

  @inline final def ++(remainder: Path) = { this.remainder = remainder; this }

  @inline final def ++(request: Request) = { this.request = request; this }

  @inline final def ++(response: Response) = { this.response = response; this }

}

/**
 *
 */
object Context {

  @inline def apply() = new Context(null, null, null, null, null)

}
