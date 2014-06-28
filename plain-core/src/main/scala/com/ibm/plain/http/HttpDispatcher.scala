package com.ibm

package plain

package http

import com.typesafe.config.Config

/**
 *
 */
abstract class HttpDispatcher[A]

  extends HttpProcessor[A] {

  def initialize: HttpDispatcher[A]

  def name = n

  def config = c

  def set(name: String, config: Config) = {
    n = name
    c = config
  }

  private[this] final var n: String = null

  private[this] final var c: Config = null

}

