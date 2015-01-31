package com.ibm

package plain

package aio

package client

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }
import java.util.concurrent.{ CountDownLatch, TimeUnit }
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

  private final val outerhandler: Handler[Integer, Any],

  private final val attachment: Any,

  private[this] final val progressfun: Int ⇒ Unit)

    extends Logger {

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
    latch.await(defaulttimeout, TimeUnit.MILLISECONDS)
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
      releaseByteBuffer(buffer)
      source.close
      destination.close
    }
  }

  @inline private final def progress(processed: Int) = if (null != progressfun) progressfun(processed)

  @inline private final def available = buffer.remaining

  private final val latch = new CountDownLatch(1)

  private[this] final val buffer = defaultByteBuffer

  private[this] final val isopen = new AtomicBoolean(true)

  private[this] final val defaulttimeout = 300000

}

/**
 *
 */
object ClientExchange

    extends Logger {

  final def apply(source: Conduit, destination: Conduit, handler: Handler[Integer, Any], attachment: Any, progress: Int ⇒ Unit) = new ClientExchange(source, destination, handler, attachment, progress)

  final def apply(source: Conduit, destination: Conduit) = new ClientExchange(source, destination, null, null, null)

  /**
   * Basic handler.
   */
  sealed abstract class ReleaseHandler

      extends Handler[Integer, ClientExchange] {

    final def failed(e: Throwable, exchange: ClientExchange) = {
      e match {
        case e: IOException ⇒ error(e)
        case e ⇒ e.printStackTrace; error(e)
      }
      exchange.latch.countDown
      if (null != exchange.outerhandler) exchange.outerhandler.failed(e, exchange.attachment)
    }

    @inline final def completed(processed: Integer, exchange: ClientExchange) = try {
      doComplete(processed, exchange)
    } catch {
      case e: Throwable ⇒
        error(s"completed failed : $e")
        failed(e, exchange)
    }

    protected[this] def doComplete(processed: Integer, exchange: ClientExchange)

  }

  /**
   * Handling tranfers.
   */
  object TransferReadHandler

      extends ReleaseHandler {

    @inline final def doComplete(processed: Integer, exchange: ClientExchange) = {
      trace(s"read completed : processed = $processed")
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
      trace(s"write completed : processed = $processed")
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
      trace(s"close completed : processed = $processed")
      if (0 < exchange.available) {
        exchange.writeTransfer(this, false)
      } else {
        if (0 < exchange.latch.getCount) {
          exchange.closeTransfer
          exchange.latch.countDown
          if (null != exchange.outerhandler) exchange.outerhandler.completed(0, exchange.attachment)
        }
      }
    }
  }

}
