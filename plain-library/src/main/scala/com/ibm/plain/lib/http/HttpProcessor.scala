package com.ibm.plain

package lib

package http

import java.io.IOException

import com.ibm.plain.lib.aio.{ AioProcessor, Iteratee }

import Status.{ ServerError, Success }
import aio.Io
import aio.Iteratee.{ Done, Error }

/**
 *
 */
final class HttpProcessor

  extends AioProcessor[Request, Response] {

  @inline final def process(io: Io) = {
    val request = io.iteratee.result
    // we should do some dispatching here
    // we might not come here as .result may throw an exception
    completed(Response(Success.`200`), io)
  }

  @inline final def completed(response: Response, io: Io) = {
    import io._
    val it: Iteratee[Io, Response] = Done(response)
    k(io ++ it)
  }

  @inline final def failed(e: Throwable, io: Io) = {
    import io._
    val it: Iteratee[Io, Response] = e match {
      case e: IOException ⇒ Error[Io](e)
      case status: Status ⇒ Done(Response(status))
      case _ ⇒ Done(Response(ServerError.`500`))
    }
    k(io ++ it)
  }

}

