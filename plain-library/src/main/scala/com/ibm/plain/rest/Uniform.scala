package com.ibm

package plain

package rest

import com.typesafe.config.Config

import http.{ Request, Response, HttpProcessor }

/**
 *
 */
trait Uniform

  extends HttpProcessor[Context]

/**
 *
 */
trait StaticUniform

  extends Uniform {

  private[rest] def init(config: Config) = ()

}
