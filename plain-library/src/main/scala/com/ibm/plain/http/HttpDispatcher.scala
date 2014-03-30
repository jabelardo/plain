package com.ibm

package plain

package http

import com.typesafe.config.Config

/**
 *
 */
abstract class HttpDispatcher[A]

  extends HttpProcessor[A] {

  def init: HttpDispatcher[A]

  def name = localname

  def config = localconfig

  def init(name: String, config: Config) = {
    localname = name
    localconfig = config
  }

  private[this] final var localname: String = null

  private[this] final var localconfig: Config = null

}

