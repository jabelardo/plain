package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }
import java.util.concurrent.atomic.AtomicBoolean

import logging.Logger
import Input._
import Iteratee._

/**
 * An Exchange starts with an accept on a AsynchronousSocketChannel. It is initialized with the corresponding AsychronousSocketChannel.
 * An Exchanges consists of Read, Process and Write operations. It will always start with a Read operation and (except in error situations) will
 * end with a Write operation. All operations Read, Process and Write can happen simultanously. An Exchange transports Messages to and from the channel
 * endpoints. Both endpoints can be sender and receiver. A Message send from the initiator of the Exchange, the Client, is called an InMessage, a Message send
 * from the accepting endpoint of the Exchange, the Server, is called an OutMessage. A typical example for Http is a Request for an InMessage and a Response for
 * an OutMessage. The underlying AsynchronousSocketChannel is by default kept open and allows the Exchange to read, process and write many Messages, until one of
 * the Endpoints requests a Close on the Exchange. In this case the running read, process, write operation will be completed and the underlying Channel will
 * be closed then.
 */

/**
 *
 */
trait EndPoint

/**
 *
 */
import Exchange.{ ExchangeHandler, ReadIteratee, warn, debug }

/**
 *
 */
final class Exchange[A] private (

  private[this] final val attachment: A,

  private[this] final val socketchannel: SocketChannel,

  private[this] final val readiteratee: ReadIteratee[A],

  private[this] final val readbuffer: ByteBuffer,

  private[this] final val writebuffer: ByteBuffer) {

  @inline private final def apply(input: Input[Exchange[A]]): (Iteratee[Exchange[A], _], Input[Exchange[A]]) = {
    readbuffer.flip
    writebuffer.clear
    readiteratee(input)
  }

  @inline private final def read(handler: ExchangeHandler[A]) = {
    readbuffer.clear
    socketchannel.read(readbuffer, this, handler)
  }

  @inline private final def write(handler: ExchangeHandler[A], flip: Boolean) = {
    if (flip) writebuffer.flip
    socketchannel.write(writebuffer, this, handler)
  }

  @inline private final def ++(that: Exchange[A]) = { throw new NotImplementedError }

  @inline private final def ++(contiteratee: Cont[Exchange[A], _]) = { this.contiteratee = contiteratee; this }

  @inline private final def ++(responsebuffer: ByteBuffer) = { this.writebuffer.put(responsebuffer); this }

  @inline private final def close = keepalive = false

  @inline private final def keepAlive = keepalive

  @inline private final def release(e: Throwable) = if (released.compareAndSet(false, true)) {
    e match {
      case null ⇒
      case _: java.io.IOException ⇒
      case _: java.lang.IllegalStateException ⇒ warn(e.toString)
      case e ⇒
        debug(text.stackTraceToString(e))
        warn(e.toString)
    }
    releaseByteBuffer(readbuffer)
    releaseByteBuffer(writebuffer)
    if (socketchannel.isOpen) socketchannel.close
  }

  private[this] final var released = new AtomicBoolean(false)

  private[this] final var keepalive = true

  private[this] final var contiteratee: Cont[Exchange[A], _] = null

}

/**
 *
 */
object Exchange

  extends Logger {

  /**
   * Constructors.
   */
  final def apply[A](

    attachment: A,

    socketchannel: SocketChannel,

    readiteratee: ReadIteratee[A],

    readbuffer: ByteBuffer,

    writebuffer: ByteBuffer) = new Exchange(attachment, socketchannel, readiteratee, readbuffer, writebuffer)

  /**
   * Type definitions.
   */
  type ReadIteratee[A] = Iteratee[Exchange[A], _]

  type ExchangeHandler[A] = Handler[Integer, Exchange[A]]

  /**
   * Helpers.
   */
  @inline def unhandled(e: Any) = error("Unhandled, may need attention : " + e)

  @inline private def ignore = debug("Ignored.")

  /**
   * The core of all asynchronous IO starting with an Accept at the very bottom.
   */
  final def loop[A](

    server: ServerChannel,

    attachment: A,

    readiteratee: ReadIteratee[A],

    processor: Any): Unit = {

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
        read(Exchange(attachment, socketchannel, readiteratee, defaultByteBuffer, defaultByteBuffer))
      }

      @inline final def failed(e: Throwable, ignore: Null) = {
        if (server.isOpen) {
          accept
          e match {
            case _: IOException ⇒ debug("Accept failed : " + e + "(" + server + ")")
            case _: Throwable ⇒ warn("Accept failed : " + e + "(" + server + ")")
          }
        }
      }

    }

    sealed abstract class ReleaseHandler

      extends ExchangeHandler[A] {

      @inline def failed(e: Throwable, exchange: Exchange[A]) = exchange.release(e)

    }

    /**
     * Handling reads.
     */
    object ReadHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          exchange(if (0 == processed) Eof else Elem(exchange)) match {
            case (cont @ Cont(_), Empty) ⇒
              println("!!!!!!!!! not handled")
            case (e @ Done(_), Elem(exchange)) ⇒
              exchange ++ defaultresponse.duplicate
              write(exchange)
            case (e @ Error(_), Elem(exchange)) ⇒
              write(exchange)
            case (_, Eof) ⇒
              ignore
            case e ⇒
              unhandled(e)
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * Handling writes.
     */
    object WriteHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = ()

    }

    /**
     * The AIO methods.
     */
    @inline def accept = server.accept(null, AcceptHandler)

    @inline def read(exchange: Exchange[A]) = exchange.read(ReadHandler)

    @inline def write(exchange: Exchange[A], flip: Boolean = true) = exchange.write(WriteHandler, flip)

    /**
     * Now let's get started.
     */
    accept

  } // loop

  final val defaultresponse = {
    val buf = ByteBuffer.allocateDirect(1000)
    buf.put("""HTTP/1.1 200 OK
Server: plain 1.0.0
Date: Sun, 23 Mar 2014 17:10:48 GMT
Content-Type: text/plain; charset=UTF-8
Content-Length: 5

pong!""".getBytes)
    buf.flip
    buf
  }

}
