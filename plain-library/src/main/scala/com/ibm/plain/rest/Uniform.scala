package com.ibm

package plain

package rest

import com.typesafe.config.Config

import http.{ Request, Response, HttpProcessor }

/**
 * A basic implementation for Uniform using Http.
 */
trait BaseResource

  extends HttpProcessor[Context]

/**
 * Marker to indicate that there is only one instance of this resource which is also thread-safe.
 */
trait StaticResource {

  private[rest] def init(config: Config) = ()

}
