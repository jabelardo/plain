package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset

import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import conduit.{ SocketChannelConduit, TerminatingConduit }
import io.PrintWriter
import logging.Logger

/**
 * Putting it all together.
 */
trait Exchange[A]

  extends ExchangeAccess[A]

  with ExchangeAio[A]

  with ExchangeIo[A]

/**
 * Public interface of the aio.Exchange
 */
trait ExchangeAccess[A] {

  def attachment: Option[A]

  def socketChannel: SocketChannelConduit

  def iteratee: ExchangeIteratee[A]

  def inMessage: InMessage

  def outMessage: OutMessage

  def printWriter: PrintWriter

  def remaining: Int

  def available: Int

  def keepAlive: Boolean

  def transferFrom(source: TerminatingConduit): Unit

  def transferTo(destination: TerminatingConduit, completed: A ⇒ Unit): Nothing

  /**
   * Setters.
   */

  def ++(attachment: Option[A]): Exchange[A]

  def ++(iteratee: ExchangeIteratee[A]): Exchange[A]

  def ++(inmessage: InMessage): Exchange[A]

  def ++(outmessage: OutMessage): Exchange[A]

  def ++(printwriter: PrintWriter): Exchange[A]

  /**
   * Some exceptional access.
   */

  private[plain] def writeBuffer: ByteBuffer

  private[plain] def setDestination(destination: Channel)

  private[plain] def setSource(source: Channel)

}

/**
 * Private interface for aio methods.
 */
private[aio] trait ExchangeAio[A] {

  private[aio] def apply(input: Input[ExchangeIo[A]], flip: Boolean): (ExchangeIteratee[A], Input[ExchangeIo[A]])

  private[aio] def cache(cachediteratee: Done[ExchangeIo[A], _])

  private[aio] def read(handler: ExchangeHandler[A])

  private[aio] def write(handler: ExchangeHandler[A], flip: Boolean)

  private[aio] def readTransfer(handler: ExchangeHandler[A])

  private[aio] def writeTransfer(handler: ExchangeHandler[A], flip: Boolean)

  private[aio] def closeTransfer

  private[aio] def isTransferFrom: Boolean

  /**
   * Helpers
   */

  private[aio] def close

  private[aio] def reset

  private[aio] def release(e: Throwable)

  private[aio] def hasError: Boolean

}

/**
 * The inner low-level aio methods.
 */
trait ExchangeIo[A] {

  private[aio] def length: Int

  private[aio] def decode(characterset: Charset, lowercase: Boolean): String

  private[aio] def consume: Array[Byte]

  private[aio] def take(n: Int): ExchangeIo[A]

  private[aio] def peek(n: Int): ExchangeIo[A]

  private[aio] def peek: Byte

  private[aio] def drop(n: Int): ExchangeIo[A]

  private[aio] def indexOf(b: Byte): Int

  private[aio] def span(p: Int ⇒ Boolean): (Int, Int)

  private[aio] def ++(that: ExchangeIo[A]): ExchangeIo[A]

}

/**
 *
 */
object Exchange

  extends Logger {

  /**
   * Constructor.
   */
  final def apply[A](

    socketchannel: SocketChannelConduit,

    readiteratee: ExchangeIteratee[A],

    readbuffer: ByteBuffer,

    writebuffer: ByteBuffer) = new ExchangeImpl[A](socketchannel, readiteratee, readbuffer, writebuffer)

  /**
   * The core of all asynchronous IO starting with an accept at the very bottom.
   */
  final def loop[A](

    serverchannel: ServerChannel,

    readiteratee: ExchangeIteratee[A],

    processor: Processor[A]): Unit = {

    /**
     * Helpers.
     */
    @inline def unhandled(e: Any) = { dumpStack; warn("Unhandled, may need attention : " + e) }

    @inline def ignore = ()

    /**
     * The AIO handlers.
     */

    /**
     * Handling accepts.
     */
    object AcceptHandler

      extends Handler[SocketChannel, Null] {

      @inline final def completed(socketchannel: SocketChannel, ignore: Null) = {
        accept
        read(Exchange[A](SocketChannelConduit(socketchannel), readiteratee, defaultByteBuffer, defaultByteBuffer))
      }

      @inline final def failed(e: Throwable, ignore: Null) = {
        def msg = "accept failed : " + e + "(" + serverchannel + ")"
        if (serverchannel.isOpen) {
          accept
          e match {
            case _: IOException ⇒ debug(msg)
            case _: Throwable   ⇒ warn(msg)
          }
        }
      }

    }

    sealed abstract class ReleaseHandler

      extends ExchangeHandler[A] {

      @inline final def failed(e: Throwable, exchange: Exchange[A]) = {
        e match {
          case e: IOException ⇒ ignore
          case e              ⇒ e.printStackTrace; trace(e)
        }
        exchange.release(e)
      }

      @inline def completed(processed: Integer, exchange: Exchange[A]) = try {
        doComplete(processed, exchange)
      } catch { case e: Throwable ⇒ failed(e, exchange) }

      protected[this] def doComplete(processed: Integer, exchange: Exchange[A]): Unit

    }

    /**
     * Handling reads.
     */
    object ReadHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        exchange(if (0 >= processed) Eof else Elem(exchange), processed != Int.MaxValue) match {
          case (cont @ Cont(_), Empty) ⇒
            read(exchange ++ cont)
          case (e @ Done(in: InMessage), Elem(_)) ⇒
            exchange.cache(e)
            process(exchange ++ in)
          case (e @ Done(a), Elem(_)) ⇒
            unhandled(e)
            exchange.release(null)
          case (e @ Error(_), Elem(_)) ⇒
            exchange.reset
            process(exchange ++ e)
          case (_, Eof) ⇒
            exchange.release(null)
          case e ⇒
            unhandled(e)
        }
      }

    }

    /**
     * Processing InMessage to OutMessage.
     */
    object ProcessHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        exchange.iteratee match {
          case Done(_) ⇒
            exchange.outMessage.render(exchange) match {
              case Done(_) ⇒
                if (0 < exchange.remaining) {
                  ReadHandler.completed(Int.MaxValue, exchange)
                } else {
                  write(exchange)
                }
              case Cont(_) ⇒
                transferFrom(exchange)
              case e ⇒
                unhandled(e)
            }
          case e @ Cont(_) ⇒
            transferTo(exchange)
          case Error(e) ⇒
            unhandled(e)
          case e ⇒
            unhandled(e)
        }
      }

    }

    /**
     * Handling writes.
     */
    object WriteHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 == exchange.available || exchange.hasError) {
          exchange.reset
          read(exchange)
        } else {
          write(exchange, false)
        }
      }

    }

    /**
     * Handling tranfers.
     */
    object TransferReadHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 >= processed) {
          exchange.writeTransfer(TransferCloseHandler, true)
        } else {
          exchange.writeTransfer(TransferWriteHandler, true)
        }
      }

    }

    object TransferWriteHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 < exchange.available) {
          exchange.writeTransfer(this, false)
        } else {
          exchange.readTransfer(TransferReadHandler)
        }
      }
    }

    object TransferCloseHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 < exchange.available) {
          exchange.writeTransfer(this, false)
        } else {
          exchange.closeTransfer
          if (exchange.isTransferFrom) {
            read(exchange)
          } else {
            ProcessHandler.completed(0, exchange)
          }
        }
      }
    }

    /**
     * The AIO methods.
     */
    @inline def accept = serverchannel.accept(null, AcceptHandler)

    @inline def read(exchange: Exchange[A]) = exchange.read(ReadHandler)

    @inline def process(exchange: Exchange[A]): Unit = processor.process(exchange, ProcessHandler)

    @inline def write(exchange: Exchange[A], flip: Boolean = true) = exchange.write(WriteHandler, flip)

    @inline def transferFrom(exchange: Exchange[A]) = exchange.write(TransferWriteHandler, true)

    @inline def transferTo(exchange: Exchange[A]) = exchange.readTransfer(TransferReadHandler)

    /**
     * Now, let's get started.
     */
    accept

  } // loop

}

