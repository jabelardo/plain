package com.ibm

package plain

package rest

import com.typesafe.config.Config

import http.HttpProcessor

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

  def init(config: Config)

}
