package com.ibm

package plain

package rest

import aio.Io
import http.Request.{ Path, Variables }

/**
 * A wrapper to hold shared context among Uniforms.
 */
final case class Context(

  var io: Io,

  var parent: Uniform,

  var variables: Variables,

  var remainder: Path) {

  @inline final def ++(io: Io) = { this.io = io; this }

  @inline final def ++(parent: Uniform) = { this.parent = parent; this }

  @inline final def ++(variables: Variables) = { this.variables = variables; this }

  @inline final def ++(remainder: Path) = { this.remainder = remainder; this }

}

/**
 *
 */
object Context {

  def apply(io: Io): Context = new Context(io, null, null, null)

}