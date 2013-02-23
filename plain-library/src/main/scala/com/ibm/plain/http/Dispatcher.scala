package com.ibm

package plain

package http

import scala.reflect._
import scala.reflect.runtime.universe._

import aio.Io

/**
 * This could be the RestDispatcher, for instance. As instances of this class will be created via Class.newInstance
 * it provides some ugly vars to be set by this framework.
 */
abstract class Dispatcher

  extends Processor {

  /**
   *
   */
  def dispatch(request: Request, io: Io)

  @inline final def process(io: Io) = dispatch(io.iteratee.result.asInstanceOf[Request], io)

  final var name: String = null

}
