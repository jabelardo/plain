package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Arrays

import scala.math.min

import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import logging.Logger
import io.PrintWriter

/**
 *
 */
final class Exchange[A] private (

  private[this] final val channel: Channel,

  private[this] final val readiteratee: ExchangeIteratee[A],

  private[this] final val readbuffer: ByteBuffer,

  private[this] final val writebuffer: ByteBuffer) {

  import Exchange._

  /**
   * Getters.
   */
  @inline final def attachment = attachmnt

  @inline final def socketChannel = channel

  @inline final def iteratee = currentiteratee

  @inline private[plain] final def writeBuffer = writebuffer

  @inline final def inMessage = inmessage

  @inline final def outMessage = outmessage

  @inline final def printWriter = printwriter

  @inline final def length: Int = readbuffer.remaining

  @inline final def available: Int = writebuffer.remaining

  @inline final def cached = null != cachedarray

  @inline final def keepAlive = null == inmessage || inmessage.keepalive

  final def encodeOnce(encoder: Encoder, length: Int) = {
    writebuffer.limit(writebuffer.position)
    writebuffer.position(writebuffer.position - length)
    encoder.encode(writebuffer)
    encoder.finish(writebuffer)
    writebuffer.position(writebuffer.limit)
    writebuffer.limit(writebuffer.capacity)
  }

  final def transferFrom(source: Channel): Unit = {
    transfersource = source
    transferdestination = channel
    transfercompleted = null
  }

  final def transferTo(destination: Channel, length: Long, completed: A ⇒ Unit): Nothing = {
    transfersource = AsynchronousFixedLengthChannel(channel, readbuffer.remaining, length)
    transferdestination = destination
    transfercompleted = completed
    throw ExchangeControl
  }

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

  final def consume: Array[Byte] = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ emptyArray
      case n ⇒ readBytes(n)
    })

  @inline final def take(n: Int): Exchange[A] = {
    markLimit
    readbuffer.limit(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  @inline final def peek(n: Int): Exchange[A] = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = readbuffer.get(readbuffer.position)

  @inline final def drop(n: Int): Exchange[A] = {
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

  /**
   * Exchange[A] internals.
   */

  @inline private final def apply(input: Input[Exchange[A]], flip: Boolean): (ExchangeIteratee[A], Input[Exchange[A]]) = input match {
    case Elem(_) ⇒
      if (flip) readbuffer.flip
      val fromcache = if (null == cachedarray) {
        false
      } else if (0 < readbuffer.position && readbuffer.remaining >= cachedarray.length) {
        readbuffer.mark
        readbuffer.get(peekarray)
        if (Arrays.equals(cachedarray, peekarray)) {
          true
        } else {
          readbuffer.rewind
          setCachedArray(0)
          false
        }
      } else {
        false
      }
      if (fromcache) (cachediteratee, Elem(this)) else currentiteratee(input)
    case _ ⇒
      readbuffer.flip
      currentiteratee(input)
  }

  /**
   * This one is too clever...
   */
  @inline private final def cache(cachediteratee: Done[Exchange[A], _]) = if (0 < readbuffer.position && null == cachedarray) {
    this.cachediteratee = cachediteratee
    var len = readbuffer.position
    readbuffer.rewind
    len -= readbuffer.position
    setCachedArray(len)
    readbuffer.get(cachedarray)
    false
  } else {
    true
  }

  /**
   * Io methods.
   */

  @inline private final def read(handler: ExchangeHandler[A]) = {
    if (0 < (readbuffer.capacity - readbuffer.limit) && 0 == readbuffer.position) {
      readbuffer.position(readbuffer.limit)
      readbuffer.limit(readbuffer.capacity)
    } else {
      readbuffer.clear
    }
    channel.read(readbuffer, this, handler)
  }

  @inline private final def write(handler: ExchangeHandler[A], flip: Boolean) = {
    if (flip) writebuffer.flip
    channel.write(writebuffer, this, handler)
  }

  /**
   * Internals for transfer.
   */

  @inline private final def readDecoding(handler: ExchangeHandler[A]) = {
    readbuffer.clear
    transfersource.read(readbuffer, this, handler)
  }

  @inline private final def writeDecoding(handler: ExchangeHandler[A], flip: Boolean) = {
    if (flip) readbuffer.flip
    transferdestination.write(readbuffer, this, handler)
  }

  @inline private final def readEncoding(handler: ExchangeHandler[A]) = {
    writebuffer.clear
    writebuffer.limit(writebuffer.limit - encodingSpareBufferSize)
    transfersource.read(writebuffer, this, handler)
  }

  @inline private final def writeEncoding(handler: ExchangeHandler[A], flip: Boolean, encode: Int) = {
    if (flip) writebuffer.flip
    outmessage.encoder match {
      case Some(encoder) ⇒ encode match {
        case -1 ⇒
          writebuffer.clear
          encoder.finish(writebuffer)
        case 0 ⇒
        case 1 ⇒
          encoder.encode(writebuffer)
          writebuffer.flip
      }
      case _ ⇒
    }
    transferdestination.write(writebuffer, this, handler)
  }

  @inline private final def transferClose = {
    if (channel ne transferdestination) transferdestination.close
    if (null != transfercompleted) transfercompleted(attachment.get)
    transfersource = null
    transferdestination = null
    transfercompleted = null
    currentiteratee = Done[Exchange[A], Null](null)
  }

  @inline private final def hasError = currentiteratee.isInstanceOf[Error[_]]

  @inline private final def written = 0 == writebuffer.remaining

  /**
   * ++ setters.
   */

  @inline final def ++(that: Exchange[A]) = {
    if (this eq that) that else unsupported
  }

  @inline final def ++(attachment: Option[A]) = { this.attachmnt = attachment; this }

  @inline final def ++(iteratee: ExchangeIteratee[A]) = { this.currentiteratee = iteratee; this }

  @inline final def ++(inmessage: InMessage) = { this.inmessage = inmessage; this }

  @inline final def ++(outmessage: OutMessage) = { this.outmessage = outmessage; this }

  @inline final def ++(printwriter: PrintWriter) = { this.printwriter = printwriter; this }

  override final def toString = s"""read $readbuffer
write $writebuffer"""

  /**
   * Internals.
   */

  @inline private final def close = keepalive = false

  @inline private final def reset = {
    readbuffer.clear
    writebuffer.clear
    currentiteratee = readiteratee
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

  @inline private[this] final def setCachedArray(length: Int) = if (0 < length) {
    cachedarray = new Array[Byte](length)
    peekarray = new Array[Byte](length)
  } else {
    cachediteratee = null
    cachedarray = null
    peekarray = null
  }

  private[this] final var released = new AtomicBoolean(false)

  private[this] final var keepalive = true

  private[this] final var currentiteratee: ExchangeIteratee[A] = readiteratee

  private[this] final var attachmnt: Option[A] = None

  private[this] final var cachediteratee: ExchangeIteratee[A] = null

  private[this] final var cachedarray: Array[Byte] = null

  private[this] final var peekarray: Array[Byte] = null

  private[this] final var inmessage: InMessage = null

  private[this] final var outmessage: OutMessage = null

  private[this] final var printwriter: PrintWriter = null

  private[this] final var transfersource: Channel = null

  private[this] final var transferdestination: Channel = null

  private[this] final var transfercompleted: A ⇒ Unit = null

}

/**
 * ******************************************************************************************************************
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

    writebuffer: ByteBuffer) = new Exchange[A](channel, readiteratee, readbuffer, writebuffer)

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
        read(Exchange[A](SocketChannelWithTimeout(socketchannel), readiteratee, defaultByteBuffer, defaultByteBuffer))
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
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * Processing InMessage to OutMessage.
     */
    object ProcessHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
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
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * Handling writes.
     */
    object WriteHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else if (exchange.written || exchange.hasError) {
          exchange.reset
          read(exchange)
        } else {
          write(exchange, false)
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * Handling tranfers from the channel "to" a destination, eventually with decoding.
     */
    object DecodeReadHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          if (0 == processed) {
            exchange.writeDecoding(DecodeCloseHandler, true)
          } else {
            exchange.writeDecoding(DecodeWriteHandler, true)
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    object DecodeWriteHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          if (0 < exchange.length) {
            exchange.writeDecoding(this, false)
          } else {
            exchange.readDecoding(DecodeReadHandler)
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    object DecodeCloseHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          if (0 < exchange.length) {
            exchange.writeDecoding(this, false)
          } else {
            exchange.transferClose
            ProcessHandler.completed(0, exchange)
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }
    }

    /**
     * Handling tranfers "from" a source to the channel, eventually with encoding.
     */
    object EncodeReadHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.writeEncoding(EncodeCloseHandler, true, -1)
        } else {
          exchange.writeEncoding(EncodeWriteHandler, true, 1)
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    object EncodeWriteHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          if (0 < exchange.available) {
            exchange.writeEncoding(this, false, 0)
          } else {
            exchange.readEncoding(EncodeReadHandler)
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    object EncodeCloseHandler

      extends ReleaseHandler {

      @inline final def completed(processed: Integer, exchange: Exchange[A]) = try {
        if (0 > processed) {
          exchange.release(null)
        } else {
          if (0 < exchange.available) {
            exchange.writeEncoding(this, false, 0)
          } else {
            exchange.transferClose
            read(exchange) // next please
          }
        }
      } catch { case e: Throwable ⇒ exchange.release(e) }

    }

    /**
     * The AIO methods.
     */
    @inline def accept = serverchannel.accept(null, AcceptHandler)

    @inline def read(exchange: Exchange[A]) = exchange.read(ReadHandler)

    @inline def process(exchange: Exchange[A]): Unit = processor.process(exchange, ProcessHandler)

    @inline def write(exchange: Exchange[A], flip: Boolean = true) = exchange.write(WriteHandler, flip)

    @inline def transferTo(exchange: Exchange[A]) = exchange.writeDecoding(DecodeWriteHandler, false)

    @inline def transferFrom(exchange: Exchange[A]) = exchange.writeEncoding(EncodeWriteHandler, true, 0)

    /**
     * Now, let's get started.
     */
    accept

  } // loop

}
