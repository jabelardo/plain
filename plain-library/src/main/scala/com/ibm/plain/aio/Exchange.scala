package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }

import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import logging.Logger

/**
 * Putting it all together.
 */
trait Exchange[A]

  extends ExchangePublicApi[A]

  with ExchangePrivateApi[A]

  with ExchangeIo[A]

/**
 *
 */
object Exchange

  extends Logger {

  /**
   * Constructors.
   */
  final def apply[A](

    channel: Channel,

    readiteratee: ExchangeIteratee[A],

    readbuffer: ByteBuffer,

    writebuffer: ByteBuffer) = new ExchangeImpl[A](channel, readiteratee, readbuffer, writebuffer)

  /**
   * The core of all asynchronous IO starting with an Accept at the very bottom.
   */
  final def loop[A](

    serverchannel: ServerChannel,

    readiteratee: ExchangeIteratee[A],

    processor: AsynchronousProcessor[A]): Unit = {

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
        read(Exchange[A](AsynchronousSocketChannelWithTimeout(socketchannel), readiteratee, defaultByteBuffer, defaultByteBuffer))
      }

      @inline final def failed(e: Throwable, ignore: Null) = {
        if (serverchannel.isOpen) {
          accept
          e match {
            case _: IOException ⇒ debug("Accept failed : " + e + "(" + serverchannel + ")")
            case _: Throwable ⇒ warn("Accept failed : " + e + "(" + serverchannel + ")")
          }
        }
      }

    }

    sealed abstract class ReleaseHandler

      extends ExchangeHandler[A] {

      @inline final def failed(e: Throwable, exchange: Exchange[A]) = {
        e match {
          case e: IOException ⇒
          case e ⇒ e.printStackTrace
        }
        exchange.release(e)
      }

      @inline def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          isEof
        } else {
          doComplete(processed, exchange)
        }
      } catch { case e: Throwable ⇒ failed(e, exchange) }

      protected[this] def doComplete(processed: Integer, exchange: Exchange[A]): Unit

      @inline final def isEof = throw ReleaseHandler.eof

    }

    object ReleaseHandler {

      final val eof = new java.io.EOFException

    }

    /**
     * Handling reads.
     */
    object ReadHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        exchange(if (0 == processed) Eof else Elem(exchange), processed != Int.MaxValue) match {
          case (cont @ Cont(_), Empty) ⇒
            read(exchange ++ cont)
          case (e @ Done(in: InMessage), Elem(exchange)) ⇒
            exchange.cache(e)
            process(exchange ++ in)
          case (e @ Error(_), Elem(exchange)) ⇒
            exchange.reset
            process(exchange ++ e)
          case (_, Eof) ⇒
            ignore
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
            exchange.outMessage.renderHeader(exchange) match {
              case Done(_) ⇒
                if (0 < exchange.length) {
                  ReadHandler.completed(Int.MaxValue, exchange)
                } else {
                  write(exchange)
                }
              case Cont(_) ⇒
                transferFrom(exchange)
              case e ⇒ unhandled(e)
            }
          case Cont(_) ⇒
            transferTo(exchange)
          case e ⇒ unhandled(e)
        }
      }

    }

    /**
     * Handling writes.
     */
    object WriteHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (exchange.allWritten || exchange.hasError) {
          exchange.reset
          read(exchange)
        } else {
          write(exchange, false)
        }
      }

    }

    /**
     * Handling tranfers from the channel "to" a destination, eventually with decoding.
     */
    object DecodeReadHandler

      extends ReleaseHandler {

      var total = 0L

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        total += processed
        if (0 == processed) {
          exchange.writeDecoding(DecodeCloseHandler, true)
        } else {
          exchange.writeDecoding(DecodeWriteHandler, true)
        }
      }

    }

    object DecodeWriteHandler

      extends ReleaseHandler {

      var total = 0L

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        total += processed
        if (0 < exchange.length) {
          exchange.writeDecoding(this, false)
        } else {
          exchange.readDecoding(DecodeReadHandler)
        }
      }
    }

    object DecodeCloseHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 < exchange.length) {
          exchange.writeDecoding(this, false)
        } else {
          exchange.transferClose
          ProcessHandler.completed(0, exchange)
        }
      }
    }

    /**
     * Handling tranfers "from" a source to the channel, eventually with encoding.
     */
    object EncodeReadHandler

      extends ReleaseHandler {

      @inline override final def completed(processed: Integer, exchange: Exchange[A]) = try {
        doComplete(processed, exchange)
      } catch { case e: Throwable ⇒ failed(e, exchange) }

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 >= processed) {
          exchange.writeEncoding(EncodeCloseHandler, true, -1)
        } else {
          exchange.writeEncoding(EncodeWriteHandler, true, 1)
        }
      }

    }

    object EncodeWriteHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 < exchange.available) {
          exchange.writeEncoding(this, false, 0)
        } else {
          exchange.readEncoding(EncodeReadHandler)
        }
      }
    }

    object EncodeCloseHandler

      extends ReleaseHandler {

      @inline final def doComplete(processed: Integer, exchange: Exchange[A]) = {
        if (0 < exchange.available) {
          exchange.writeEncoding(this, false, 0)
        } else {
          exchange.transferClose
          read(exchange) // next please
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

    @inline def transferFrom(exchange: Exchange[A]) = exchange.writeEncoding(EncodeWriteHandler, true, 0)

    @inline def transferTo(exchange: Exchange[A]) = exchange.writeDecoding(DecodeWriteHandler, false)

    /**
     * Now, let's get started.
     */
    accept

  } // loop

  /**
   * Helpers.
   */
  @inline def unhandled(e: Any) = {
    e match {
      case e: Throwable ⇒ e.printStackTrace
      case _ ⇒
    }
    error("Unhandled, may need attention : " + e)
  }

  @inline private def ignore = debug("Ignored.")

}

/**
 *
 */
final class ExchangeImpl[A] private[aio] (

  protected[this] final val channel: Channel,

  protected[this] final val readiteratee: ExchangeIteratee[A],

  protected[this] final val readbuffer: ByteBuffer,

  protected[this] final val writebuffer: ByteBuffer)

  extends Exchange[A]

  with ExchangeApiImpl[A]

  with ExchangeIoImpl[A]

