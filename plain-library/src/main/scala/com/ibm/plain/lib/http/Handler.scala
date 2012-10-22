package com.ibm.plain

package lib

package http

import aio._
import java.io.IOException

/**
 * No delimited continuations used yet.
 */
class RequestHandler

  extends AioHandler[Request] {

  def completed(request: Request, io: Io) = {
    import io._
    //    println("moved out " + request)
    io.releaseBuffer
  }

  def failed(e: Throwable, io: Io) {
    import io._
    //    println("moved out failed " + e)
    ()
  }

}

