package com.ibm

package plain

package http

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
  def dispatch(request: Request, io: Io): Nothing

  @inline final def process(io: Io): Nothing = dispatch(io.iteratee.result.asInstanceOf[Request], io)

  final var name: String = null

}
