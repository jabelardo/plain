package com.ibm

package plain

package http

import com.typesafe.config.Config

/**
 *
 */
abstract class HttpDispatcher

  extends HttpProcessor {

  def name = name_

  def config = config_

  def init

  private[http] final var name_ : String = null

  private[http] final var config_ : Config = null

}

