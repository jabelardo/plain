package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException
import Status.{ ClientError, ServerError, ErrorStatus }
import Entity.ArrayEntity
import aio.{ AsynchronousProcessor, Exchange, ExchangeHandler, ExchangeControl, OutMessage }
import aio.Iteratee.{ Cont, Done, Error }
import logging.Logger
import text.stackTraceToString

/**
 * This is passed to aio.Io for processing the read input and produce output to be written.
 */
abstract class HttpProcessor[A]

  extends AsynchronousProcessor[A]

  with Logger {

  def completed(exchange: Exchange[A], handler: ExchangeHandler[A]): Unit = {
    exchange ++ Done[Exchange[A], OutMessage](exchange.outMessage)
    handler.completed(0, exchange)
  }

  def failed(e: Throwable, exchange: Exchange[A], handler: ExchangeHandler[A]): Unit = {
    exchange ++ (e match {
      case ExchangeControl ⇒ Cont[Exchange[A], Null](null)
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
            case e: ErrorStatus ⇒ response ++ None
            case _ ⇒
          }
          response ++ status
        })
      case e ⇒
        warn("Dispatching failed : " + e)
        debug(stackTraceToString(e))
        Done[Exchange[A], Response] {
          val e = ServerError.`500`
          val request = try exchange.inMessage.asInstanceOf[Request] catch { case _: Throwable ⇒ null }
          Response(null, e) ++ None
        }
    })
    handler.completed(0, exchange)
  }

}