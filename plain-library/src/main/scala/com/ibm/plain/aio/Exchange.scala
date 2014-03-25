package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

import scala.math.min

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
import Exchange.{ ExchangeHandler, ReadIteratee, warn, debug, emptyString, emptyArray }

/**
 *
 */
final class Exchange private (

  private[this] final val channel: Channel,

  private[this] final val readiteratee: ReadIteratee,

  private[this] final val readbuffer: ByteBuffer,

  private[this] final val writebuffer: ByteBuffer) {

  @inline private final def apply(input: Input[Exchange]): (Iteratee[Exchange, _], Input[Exchange]) = {
    readbuffer.flip
    iteratee(input)
  }

  @inline private final def read(handler: ExchangeHandler) = {
    clear
    iteratee = readiteratee
    channel.read(readbuffer, this, handler)
  }

  @inline private final def write(handler: ExchangeHandler, flip: Boolean) = {
    if (flip) writebuffer.flip
    channel.write(writebuffer, this, handler)
  }

  @inline private final def hasError = iteratee.isInstanceOf[Error[_]]

  @inline private final def written = 0 == writebuffer.remaining

  @inline final def ++(that: Exchange) = if (this eq that) that else { throw new NotImplementedError("this ne that") }

  @inline private final def ++(iteratee: ReadIteratee) = { this.iteratee = iteratee; this }

  @inline private final def ++(responsebuffer: ByteBuffer) = { this.writebuffer.put(responsebuffer); this }

  @inline private final def close = keepalive = false

  @inline private final def keepAlive = keepalive

  @inline private final def clear = {
    readbuffer.clear
    writebuffer.clear
  }

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
    if (channel.isOpen) channel.close
  }

  private[this] final var released = new AtomicBoolean(false)

  private[this] final var keepalive = true

  private[this] final var iteratee: ReadIteratee = readiteratee

  /**
   * Low level io.
   */

  final def decode(characterset: Charset, lowercase: Boolean): String = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ emptyString
      case n ⇒ readBytes(n) match {
        case a if a eq array ⇒ StringPool.get(if (lowercase) lowerAlphabet(a, 0, n) else a, n, characterset)
        case a ⇒ new String(a, 0, n, characterset)
      }
    })

  @inline final def consume: Array[Byte] = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ Io.emptyArray
      case n ⇒ readBytes(n)
    })

  @inline final def length: Int = readbuffer.remaining

  @inline final def take(n: Int): Exchange = {
    markLimit
    readbuffer.limit(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  @inline final def peek(n: Int): Exchange = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = readbuffer.get(readbuffer.position)

  @inline final def drop(n: Int): Exchange = {
    readbuffer.position(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  @inline final def indexOf(b: Byte): Int = {
    val p = readbuffer.position
    val l = readbuffer.limit
    var i = p
    while (i < l && b != readbuffer.get(i)) i += 1
    if (i == l) -1 else i - p
  }

  @inline final def span(p: Int ⇒ Boolean): (Int, Int) = {
    val pos = readbuffer.position
    val l = readbuffer.limit
    var i = pos
    while (i < l && p(readbuffer.get(i))) i += 1
    (i - pos, l - i)
  }

  @inline final def remaining = readbuffer.remaining

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (n <= StringPool.maxStringLength) { readbuffer.get(array, 0, n); array } else Array.fill(n)(readbuffer.get)

  @inline private[this] final def markLimit: Unit = limitmark = readbuffer.limit

  @inline private[this] final def markPosition: Unit = positionmark = readbuffer.position

  @inline private[this] final def advanceBuffer[WhatEver](whatever: WhatEver): WhatEver = {
    readbuffer.limit(limitmark)
    if (-1 < positionmark) { readbuffer.position(positionmark); positionmark = -1 }
    whatever
  }

  @inline private[this] final def lowerAlphabet(a: Array[Byte], offset: Int, length: Int): Array[Byte] = {
    for (i ← offset until length) { val b = a(i); if ('A' <= b && b <= 'Z') a.update(i, (b + 32).toByte) }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

  private[this] final val array = new Array[Byte](StringPool.maxStringLength)

}

/**
 *
 */
object Exchange

  extends Logger {

  /**
   * Type definitions.
   */
  type ReadIteratee = Iteratee[Exchange, _]

  type ExchangeHandler = Handler[Integer, Exchange]

  /**
   * Constructors.
   */
  final def apply(

    channel: Channel,

    readiteratee: ReadIteratee,

    readbuffer: ByteBuffer,

    writebuffer: ByteBuffer) = new Exchange(channel, readiteratee, readbuffer, writebuffer)

  /**
   * Constants.
   */
  final val emptyString = new String

  final val emptyArray = new Array[Byte](0)

  /**
   * Helpers.
   */
  @inline def unhandled(e: Any) = error("Unhandled, may need attention : " + e)

  @inline private def ignore = debug("Ignored.")

  /**
   * The core of all asynchronous IO starting with an Accept at the very bottom.
   */
  final def loop(

    serverchannel: ServerChannel,

    readiteratee: ReadIteratee,

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
        read(Exchange(SocketChannelWithTimeout(socketchannel), readiteratee, defaultByteBuffer, defaultByteBuffer))
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

      extends ExchangeHandler {

      @inline def failed(e: Throwable, exchange: Exchange) = exchange.release(e)

    }

    /**
     * Handling reads.
     */
    object ReadHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          exchange(if (0 == processed) Eof else Elem(exchange)) match {
            case (cont @ Cont(_), Empty) ⇒
              read(exchange ++ cont)
            case (e @ Done(_), Elem(exchange)) ⇒
              process(exchange ++ e)
            case (e @ Error(_), Elem(exchange)) ⇒
              exchange.clear
              process(exchange ++ e)
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

      @inline final def completed(processed: Integer, exchange: Exchange) = try {
        if (0 > processed) {
          exchange.release(null)
        } else if (exchange.written || exchange.hasError) {
          read(exchange)
        } else {
          write(exchange, false)
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * The AIO methods.
     */
    @inline def accept = serverchannel.accept(null, AcceptHandler)

    @inline def read(exchange: Exchange) = exchange.read(ReadHandler)

    @inline def process(exchange: Exchange): Unit = { exchange ++ defaultresponse.duplicate; write(exchange, true) }

    @inline def write(exchange: Exchange, flip: Boolean = true) = exchange.write(WriteHandler, flip)

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
