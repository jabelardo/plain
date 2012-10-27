package com.ibm.plain

package lib

package http

import java.io.IOException

import com.ibm.plain.lib.aio.{ AioProcessor, Iteratee }

import Status.{ ServerError, Success }
import aio.Io
import aio.Iteratee.{ Done, Error }

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract sealed class HttpProcessor

  extends AioProcessor[Request, Response] {

  @inline final def completed(response: Response, io: Io) = {
    import io._
    k(io ++ Done[Io, Response](response))
  }

  @inline final def failed(e: Throwable, io: Io) = {
    import io._
    k(io ++ (e match {
      case e: IOException ⇒ Error[Io](e)
      case status: Status ⇒ Done[Io, Response](Response(status))
      case _ ⇒ Done[Io, Response](Response(ServerError.`500`))
    }))
  }

}

/**
 * This could be the RestDispatcher, for instance. As instances of this class will be created via Class.newInstance it provides some ugly vars to be set by this framework.
 */
abstract class HttpDispatcher

  extends HttpProcessor {

  /**
   * For convenience and very fast synchronous response generation this method can directly return the response.
   * If it is necessary to compute the response 'out-of-line' i. e. asynchronously this method must return None
   * and call completed or failed asynchronously.
   */
  def dispatch(request: Request): Option[Response]

  @inline final def process(io: Io) = {
    this.io = io
    dispatch(io.iteratee.result.asInstanceOf[Request]) match {
      case None ⇒ println("unhandled dispatch")
      case Some(response) ⇒ completed(response, io)
    }
  }

  final var name: String = null

  final protected[this] var io: Io = null

}
