package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException

import aio.AsynchronousProcessor

import Status.{ ClientError, ServerError, ErrorStatus }
import Entity.ArrayEntity
import aio.{ Exchange, OutMessage }
import aio.Exchange.ExchangeHandler
import aio.Iteratee.{ Done, Error }
import logging.Logger
import text.stackTraceToString

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class HttpProcessor

  extends AsynchronousProcessor

  with Logger {

  final def completed(exchange: Exchange, handler: ExchangeHandler) = {
    exchange ++ Done[Exchange, OutMessage](exchange.outMessage)
    handler.completed(0, exchange)
  }

  def failed(e: Throwable, exchange: Exchange) = exchange ++ (e match {
    case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Exchange](e)
    case status: Status ⇒
      status match {
        case servererror: ServerError ⇒ debug("Dispatching failed : " + stackTraceToString(status))
        case _ ⇒
      }
      val request = exchange.inMessage.asInstanceOf[Request]
      val response = exchange.outMessage.asInstanceOf[Response]
      Done[Exchange, Response]({
        status match {
          case e: ErrorStatus ⇒ response ++ errorPage(e.code, e.reason, request.path.mkString("/"))
          case _ ⇒
        }
        response ++ status
      })
    case e ⇒
      info("Dispatching failed : " + e)
      debug(stackTraceToString(e))
      Done[Exchange, Response] {
        val e = ServerError.`500`
        val request = try exchange.inMessage.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
        Response(null, e) ++ errorPage(e.code, e.reason, if (null == request) "Unknown" else request.path.mkString("/"))
      }
  })

  private[this] final def errorPage(code: String, reason: String, uri: String): Entity = ArrayEntity(
    s"""                                                                                                                           
                                                                                                                          
     Error : $code                                                                                                                          
                                                                                                                           
     Message : $reason                                                                                                                    
                                                                                                                            
     Link : $uri                                                                                                                    
                                                                                                                           
""".getBytes(text.`UTF-8`), ContentType(MimeType.`text/plain`))

}

private object Processor {

}
