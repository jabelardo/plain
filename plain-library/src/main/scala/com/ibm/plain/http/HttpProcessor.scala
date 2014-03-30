package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException

import Status.{ ClientError, ServerError, ErrorStatus }
import Entity.ArrayEntity
import aio.{ AsynchronousProcessor, Exchange, ExchangeHandler, OutMessage }
import aio.Iteratee.{ Done, Error }
import logging.Logger
import text.stackTraceToString

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class HttpProcessor[A]

  extends AsynchronousProcessor[A]

  with Logger {

  def completed(exchange: Exchange[A], handler: ExchangeHandler[A]) = {
    exchange ++ Done[Exchange[A], OutMessage](exchange.outMessage)
    handler.completed(0, exchange)
  }

  def failed(e: Throwable, exchange: Exchange[A]) = exchange ++ (e match {
    case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[Exchange[A]](e)
    case status: Status ⇒
      status match {
        case servererror: ServerError ⇒ debug("Dispatching failed : " + stackTraceToString(status))
        case _ ⇒
      }
      val request = exchange.inMessage.asInstanceOf[Request]
      val response = exchange.outMessage.asInstanceOf[Response]
      Done[Exchange[A], Response]({
        status match {
          case e: ErrorStatus ⇒ response ++ errorPage(e.code, e.reason, request.path.mkString("/"))
          case _ ⇒
        }
        response ++ status
      })
    case e ⇒
      info("Dispatching failed : " + e)
      debug(stackTraceToString(e))
      Done[Exchange[A], Response] {
        val e = ServerError.`500`
        val request = try exchange.inMessage.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
        Response(null, e) ++ errorPage(e.code, e.reason, if (null == request) "Unknown" else request.path.mkString("/"))
      }
  })

  private[this] final def errorPage(code: String, reason: String, uri: String): Option[Entity] = Some(ArrayEntity(
    s"""                                                                                                                           
                                                                                                                          
     Error : $code                                                                                                                          
                                                                                                                           
     Message : $reason                                                                                                                    
                                                                                                                            
     Link : $uri                                                                                                                    
                                                                                                                           
""".getBytes(text.`UTF-8`), ContentType(MimeType.`text/plain`)))

}

private object Processor {

}
