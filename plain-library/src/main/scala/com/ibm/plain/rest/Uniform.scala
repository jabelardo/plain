package com.ibm

package plain

package rest

import com.typesafe.config.Config

import http.{ Request, Response, HttpProcessor }

/**
 *
 */
trait BaseResource

  extends HttpProcessor[Context]

/**
 *
 */
trait IsStatic

  extends BaseResource {

  private[rest] def init(config: Config) = ()

}
