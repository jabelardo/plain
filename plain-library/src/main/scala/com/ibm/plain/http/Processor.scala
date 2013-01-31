package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.channels.InterruptedByTimeoutException

import aio.{ Processor ⇒ AioProcessor }

import Status.ServerError
import aio.{ ControlCompleted, Io }
import aio.Iteratee.{ Done, Error }
import logging.HasLogger
import text.stackTraceToString

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class Processor

  extends AioProcessor[Request, Response] {

  final def completed(response: Response, io: Io): Unit = {
    import io._
    k(io ++ Done[Io, Response](response))
  }

  final def failed(e: Throwable, io: Io): Unit = {
    import io._
    e match {
      case ControlCompleted ⇒
      case _ ⇒ k(io ++ (e match {
        case e: InterruptedByTimeoutException ⇒
          println(e); Error[Io](e)
        case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Io](e)
        case status: Status ⇒ Done[Io, Response](Response(null, status))
        case e ⇒
          val log = logging.createLogger(this)
          log.info("Dispatching failed : " + e)
          if (log.isDebugEnabled) log.debug(stackTraceToString(e))
          Done[Io, Response](Response(null, ServerError.`500`))
      }))
    }
  }

}

