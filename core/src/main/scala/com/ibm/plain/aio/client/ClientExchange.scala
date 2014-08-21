package com.ibm

package plain

package aio

package client

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import aio.conduit.Conduit
import logging.Logger
import concurrent.spawn

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
    readTransfer(TransferReadHandler)
    latch.await
  }

  /**
   * Note: spawn, necessary to keep the stack depth shallow.
   */
  @inline private final def readTransfer(handler: ExchangeHandler) = {
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

  @inline private final def closeTransfer = {
    if (isopen.compareAndSet(true, false)) {
      latch.countDown
      releaseByteBuffer(buffer)
      source.close
      destination.close
    }
  }

  @inline private final def progress(processed: Int) = if (null != progressfun) progressfun(processed)

  @inline private final def available = buffer.remaining

  private[this] final val buffer = defaultByteBuffer

  private[this] final val latch = new CountDownLatch(1)

  private[this] final val isopen = new AtomicBoolean(true)

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
        case e ⇒ e.printStackTrace; error(e)
      }
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
        exchange.closeTransfer
      }
    }
  }

}