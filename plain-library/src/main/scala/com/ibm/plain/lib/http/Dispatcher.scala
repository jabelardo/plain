package com.ibm.plain

package lib

package http

import java.io.IOException

import com.ibm.plain.lib.aio.{ Iteratee }

import Status.{ ServerError, Success }
import aio.Io
import aio.Iteratee.{ Done, Error }

/**
 * This could be the RestDispatcher, for instance. As instances of this class will be created via Class.newInstance
 * it provides some ugly vars to be set by this framework.
 */
abstract class Dispatcher

  extends Processor {

  /**
   * For convenience and very fast synchronous response generation this method can directly return the response.
   * If it is necessary to compute the response 'out-of-line' i. e. asynchronously this method must return None
   * and call completed or failed asynchronously.
   */
  def dispatch(request: Request, io: Io): Nothing

  @inline final def process(io: Io): Nothing = dispatch(io.iteratee.result.asInstanceOf[Request], io)

  final var name: String = null

}
