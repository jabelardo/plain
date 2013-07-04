package com.ibm

package plain

package http

import com.typesafe.config.Config

import aio.Io
import aio.Iteratee._

/**
 * This could be the RestDispatcher, for instance. As instances of this class will be created via Class.newInstance
 * it provides some ugly vars to be set by this framework.
 */
abstract class Dispatcher

  extends Processor {

  /**
   *
   */
  def dispatch(requests: List[Request], io: Io)

  @inline final def process(io: Io) = dispatch(io.iteratee.result.asInstanceOf[List[Request]], io)

  def name = name_

  def config = config_

  def init

  private[http] final var name_ : String = null

  private[http] final var config_ : Config = null

}

