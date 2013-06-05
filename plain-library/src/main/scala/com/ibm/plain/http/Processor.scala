package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException

import aio.{ Processor ⇒ AioProcessor }

import Status.ServerError
import aio.Io
import aio.Iteratee.{ Done, Error }
import logging.HasLogger
import text.stackTraceToString

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class Processor

  extends AioProcessor[Response]

  with HasLogger {

  final def completed(response: Response, io: Io) = {
    import io._
    k(io ++ Done[Io, Response](response))
  }

  def failed(e: Throwable, io: Io) = {
    import io._
    e match {
      case _ ⇒ k(io ++ (e match {
        case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Io](e)
        case status: Status ⇒
          status match {
            case servererror: ServerError ⇒ if (log.isDebugEnabled) debug(stackTraceToString(status))
            case _ ⇒
          }
          val request = try io.payload.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
          val response = try io.payload.asInstanceOf[Response] catch { case _: Throwable ⇒ null }
          Done[Io, Response](if (null != response) response ++ status else Response(request, status))
        case e ⇒
          info("Dispatching failed : " + e)
          if (log.isDebugEnabled) debug(stackTraceToString(e))
          Done[Io, Response](Response(null, ServerError.`500`))
      }))
    }
  }

}

