package com.ibm.plain

package lib

package http

import com.ibm.plain.lib.aio.{ AioHandler, Iteratee }

import aio.{ AioHandler, Io }
import aio.Iteratee.{ Done, Error }

import Status._

/**
 * A RequestHandler handles an http request and produces an http response on completion or an error.
 */
final class RequestHandler(

  server: Server)

  extends AioHandler[Request, Response] {

  def process(iter: Iteratee[Io, Request], io: Io) = iter match {
    case Done(request) ⇒
      // process req
      io.releaseBuffer
      if (true)
        completed(null, io)
      else
        failed(ClientError.`404`, io)
    case Error(e) ⇒
      io.releaseBuffer
      failed(e, io)
    case _ ⇒
      io.releaseBuffer
      failed(ServerError.`501`, io)
  }

  def completed(response: Response, io: Io) = {
    import io._
    k(io)
  }

  def failed(e: Throwable, io: Io) {
    import io._
    k(io ++ 0.toLong)
  }

}

