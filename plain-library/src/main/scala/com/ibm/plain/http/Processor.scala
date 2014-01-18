package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException

import aio.{ Processor ⇒ AioProcessor }

import Status.{ ClientError, ServerError, ErrorStatus }
import Entity.ArrayEntity
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
    io ++ Done[Io, Response](response)
  }

  def failed(e: Throwable, io: Io) = {
    io ++ (e match {
      case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Io](e)
      case status: Status ⇒
        status match {
          case servererror: ServerError ⇒ if (log.isDebugEnabled) debug(stackTraceToString(status))
          case _ ⇒
        }
        val request = try io.message.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
        val response = try io.message.asInstanceOf[Response] catch { case _: Throwable ⇒ null }
        Done[Io, Response](if (null != response) {
          status match {
            case e: ErrorStatus ⇒ response ++ errorPage(e.code, e.reason, response.request.path.mkString("/"))
            case _ ⇒
          }
          response ++ status
        } else {
          Response(request, status) ++ errorPage(status.code, status.reason, request.path.mkString("/"))
        })
      case e ⇒
        info("Dispatching failed : " + e)
        if (log.isDebugEnabled) debug(stackTraceToString(e))
        Done[Io, Response] {
          val e = ServerError.`500`
          val request = try io.message.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
          Response(null, e) ++ errorPage(e.code, e.reason, if (null == request) "Unknown" else request.path.mkString("/"))
        }
    })
  }

  private[this] final def errorPage(code: String, reason: String, uri: String) = ArrayEntity(
    s"""                                                                                                                      
                                                                                                                     
     Error : $code                                                                                                                     
                                                                                                                      
     Message : $reason                                                                                                                    
                                                                                                                       
     Link : $uri                                                                                                               
                                                                                                                      
    """.getBytes(text.`UTF-8`), ContentType(MimeType.`text/plain`))

}

private object Processor {

}

