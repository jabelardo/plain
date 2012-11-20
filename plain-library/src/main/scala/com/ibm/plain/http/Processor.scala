package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException

import aio.{ Processor ⇒ AioProcessor }

import Status.ServerError
import aio.{ Completed, Io }
import aio.Iteratee.{ Done, Error }
import logging.HasLogger

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class Processor

  extends AioProcessor[Request, Response]

  with HasLogger {

  final def completed(response: Response, io: Io) = {
    import io._
    k(io ++ Done[Io, Response](response))
  }

  final def failed(e: Throwable, io: Io) = {
    import io._
    e match {
      case Completed ⇒
      case _ ⇒ k(io ++ (e match {
        case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Io](e)
        case status: Status ⇒ Done[Io, Response](Response(status))
        case e ⇒ info("failed : " + e); e.printStackTrace; Done[Io, Response](Response(ServerError.`500`))
      }))
    }
  }

}

