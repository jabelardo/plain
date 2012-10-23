package com.ibm.plain

package lib

package http

import java.io.IOException

import aio.{ AioProcessor, Io, Iteratee }
import aio.Iteratee.{ Done, Error }

import Status._
import Version.`HTTP/1.1`

/**
 *
 */
final class HttpProcessor

  extends AioProcessor[Request, Response] {

  final def process(io: Io) = try {
    io.iteratee match {
      case Done(request) ⇒
        if (true)
          completed(Response(Success.`200`), io)
        else
          throw ClientError.`404`
      case Error(e) ⇒
        failed(e, io)
      case _ ⇒
        failed(ServerError.`501`, io)
    }
  } catch {
    case e: Throwable ⇒ failed(e, io)
  }

  @inline final def completed(response: Response, io: Io) = {
    import io._
    val it: Iteratee[Io, Response] = Done(response)
    k(io ++ it)
  }

  def failed(e: Throwable, io: Io) = {
    import io._
    val it: Iteratee[Io, Response] = e match {
      case e: IOException ⇒ Error[Io](e)
      case status: Status ⇒ Done(Response(status))
      case _ ⇒ Done(Response(ServerError.`500`))
    }
    k(io ++ it)
  }

}

