package com.ibm

package plain

package aio

package client

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }
import java.util.concurrent.CountDownLatch

import scala.util.control.TailCalls._

import aio.conduit.Conduit
import concurrent.spawn
import logging.Logger

/**
 *
 */
final class ClientExchange private (

    private[this] final val source: Conduit,

    private[this] final val destination: Conduit,

    private[this] final val handler: Handler[Integer, ClientExchange],

    private[this] final val progressfun: Int ⇒ Unit) {

  import ClientExchange._

  type ExchangeHandler = Handler[Integer, ClientExchange]

  /**
   * Starts the transfer from source to destination and immediately returns, the handler is called at completion.
   *
   * @param handler: Handler to be called on completion or failure, can be null.
   */
  final def transfer(handler: ExchangeHandler) = {
    readTransfer(TransferReadHandler)
  }

  /**
   * Transfers from source to destination and waits for the transfer to be completed.
   */
  final def transferAndWait = {
    latch = new CountDownLatch(1)
    readTransfer(TransferReadHandler)
    latch.await
  }

  /**
   * Note: spawn
   */
  @inline private final def readTransfer(handler: ExchangeHandler) = spawn {
    buffer.clear
    source.read(buffer, this, handler)
  }

  /**
   * Note: spawn
   */
  @inline private final def writeTransfer(handler: ExchangeHandler, flip: Boolean) = spawn {
    if (flip) buffer.flip
    destination.write(buffer, this, handler)
  }

  @inline private final def closeTransfer(e: Throwable) = {
    releaseByteBuffer(buffer)
    source.close
    destination.close
    if (null != latch) latch.countDown
    if (null != handler) if (null == e) handler.completed(-1, this) else handler.failed(e, this)
  }

  @inline private final def progress(processed: Int) = if (null != progressfun) progressfun(processed)

  @inline private final def available = buffer.remaining

  private[this] final val buffer = defaultByteBuffer

  private[this] final var latch: CountDownLatch = null

}

/**
 *
 */
object ClientExchange

    extends Logger {

  final def apply(source: Conduit, destination: Conduit, handler: Handler[Integer, ClientExchange], progress: Int ⇒ Unit) = new ClientExchange(source, destination, handler, progress)

  final def apply(source: Conduit, destination: Conduit) = new ClientExchange(source, destination, null, null)

  /**
   * Basic handler.
   */
  sealed abstract class ReleaseHandler

      extends Handler[Integer, ClientExchange] {

    final def failed(e: Throwable, exchange: ClientExchange) = {
      e match {
        case e: IOException ⇒
        case e ⇒ trace(e)
      }
      exchange.closeTransfer(e)
    }

    @inline final def completed(processed: Integer, exchange: ClientExchange) = try {
      doComplete(processed, exchange)
    } catch { case e: Throwable ⇒ failed(e, exchange) }

    protected[this] def doComplete(processed: Integer, exchange: ClientExchange)

  }

  /**
   * Handling tranfers.
   */
  object TransferReadHandler

      extends ReleaseHandler {

    @inline final def doComplete(processed: Integer, exchange: ClientExchange) = {
      exchange.progress(processed)
      if (0 >= processed) {
        exchange.writeTransfer(TransferCloseHandler, true)
      } else {
        exchange.writeTransfer(TransferWriteHandler, true)
      }
    }

  }

  object TransferWriteHandler

      extends ReleaseHandler {

    @inline final def doComplete(processed: Integer, exchange: ClientExchange) = {
      if (0 < exchange.available) {
        exchange.writeTransfer(this, false)
      } else {
        exchange.readTransfer(TransferReadHandler)
      }
    }
  }

  object TransferCloseHandler

      extends ReleaseHandler {

    @inline final def doComplete(processed: Integer, exchange: ClientExchange) = {
      if (0 < exchange.available) {
        exchange.writeTransfer(this, false)
      } else {
        exchange.closeTransfer(null)
      }
    }
  }

}