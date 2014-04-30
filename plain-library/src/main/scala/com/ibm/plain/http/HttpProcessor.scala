package com.ibm

package plain

package http

import java.io.IOException
import java.nio.file.FileSystemException
import Status.{ ClientError, ServerError, ErrorStatus }
import Entity.ArrayEntity
import aio.{ AsynchronousProcessor, Exchange, ExchangeIo, ExchangeHandler, ExchangeControl, OutMessage }
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
    exchange ++ Done[ExchangeIo[A], OutMessage](exchange.outMessage)
    handler.completed(0, exchange)
  }

  def failed(e: Throwable, exchange: Exchange[A], handler: ExchangeHandler[A]): Unit = {
    exchange ++ (e match {
      case ExchangeControl ⇒ Cont[ExchangeIo[A], Null](null)
      case e: IOException if !e.isInstanceOf[FileSystemException] ⇒ Error[ExchangeIo[A]](e)
      case status: Status ⇒
        status match {
          case servererror: ServerError ⇒ debug("Dispatching failed : " + stackTraceToString(status))
          case _ ⇒
        }
        val response = exchange.outMessage.asInstanceOf[Response]
        Done[ExchangeIo[A], Response]({
          status match {
            case e: ErrorStatus ⇒ response ++ None
            case _ ⇒
          }
          response ++ status
        })
      case e ⇒
        warn("Dispatching failed : " + e)
        debug(stackTraceToString(e))

        Done[ExchangeIo[A], Response] {
          exchange.outMessage.asInstanceOf[Response] ++ ServerError.`500` ++ None
        }
    })
    handler.completed(0, exchange)
  }

}