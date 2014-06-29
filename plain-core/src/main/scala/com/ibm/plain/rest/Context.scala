package com.ibm

package plain

package rest

import com.typesafe.config.Config

import aio.Exchange
import http.{ Request, Response }
import http.Request.{ Path, Variables }
import Resource.MethodBody

/**
 * A wrapper to hold shared context among Uniforms.
 */
final case class Context(

    var config: Config,

    var variables: Variables,

    var remainder: Path,

    var request: Request,

    var response: Response) {

  def this(request: Request) = this(null, null, null, request, null)

  @inline final def ++(config: Config) = { this.config = config; this }

  @inline final def ++(variables: Variables) = { this.variables = variables; this }

  @inline final def ++(remainder: Path) = { this.remainder = remainder; this }

  @inline final def ++(request: Request) = { this.request = request; this }

  @inline final def ++(response: Response) = { this.response = response; this }

}
