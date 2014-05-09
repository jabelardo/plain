package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousSocketChannel ⇒ SocketChannel }
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean

import Iteratee.{ Done, Error }
import Input.Elem
import io.PrintWriter
import logging.Logger
import conduits._

/**
 * Public interface of the aio.Exchange
 */
trait ExchangePublicApi[A] {

  def attachment: Option[A]

  def socketChannel: SocketChannelConduit

  def iteratee: ExchangeIteratee[A]

  def inMessage: InMessage

  def outMessage: OutMessage

  def printWriter: PrintWriter

  def remaining: Int

  def available: Int

  def keepAlive: Boolean

  def encodeOnce(encoder: Encoder, length: Int): Unit

  def transferFrom(source: Channel): Unit

  def transferTo(destination: Channel, length: Long, completed: A ⇒ Unit): Nothing

  /**
   * Setters.
   */

  def ++(attachment: Option[A]): Exchange[A]

  def ++(iteratee: ExchangeIteratee[A]): Exchange[A]

  def ++(inmessage: InMessage): Exchange[A]

  def ++(outmessage: OutMessage): Exchange[A]

  def ++(printwriter: PrintWriter): Exchange[A]

  /**
   * Some exception access.
   */

  private[plain] def writeBuffer: ByteBuffer

}

/**
 * Private interface.
 */
private[aio] trait ExchangePrivateApi[A] {

  /**
   * Aio methods.
   */

  private[aio] def apply(input: Input[ExchangeIo[A]], flip: Boolean): (ExchangeIteratee[A], Input[ExchangeIo[A]])

  private[aio] def cache(cachediteratee: Done[ExchangeIo[A], _])

  private[aio] def read(handler: ExchangeHandler[A])

  private[aio] def write(handler: ExchangeHandler[A], flip: Boolean)

  private[aio] def readDecoding(handler: ExchangeHandler[A])

  private[aio] def writeDecoding(handler: ExchangeHandler[A], flip: Boolean)

  private[aio] def readEncoding(handler: ExchangeHandler[A])

  private[aio] def writeEncoding(handler: ExchangeHandler[A], flip: Boolean, encode: Int)

  private[aio] def transferClose

  /**
   * Helpers
   */

  private[aio] def close

  private[aio] def reset

  private[aio] def release(e: Throwable)

  private[aio] def hasError: Boolean

}

/**
 * Some statics.
 */
object ExchangePublicApi extends Logger

/**
 *
 */
trait ExchangeApiImpl[A]

  extends ExchangePublicApi[A]

  with ExchangePrivateApi[A] {

  self: Exchange[A] ⇒

  import ExchangePublicApi._

  @inline final def ++(attachment: Option[A]) = { innerattachment = attachment; this }

  @inline final def ++(iteratee: ExchangeIteratee[A]) = { this.currentiteratee = iteratee; this }

  @inline final def ++(inmessage: InMessage) = { this.inmessage = inmessage; this }

  @inline final def ++(outmessage: OutMessage) = { this.outmessage = outmessage; this }

  @inline final def ++(printwriter: PrintWriter) = { this.printwriter = printwriter; this }

  @inline final def attachment = innerattachment

  @inline final def socketChannel = socketchannel

  @inline final def iteratee = currentiteratee

  @inline private[plain] final def writeBuffer = writebuffer

  @inline final def inMessage = inmessage

  @inline final def outMessage = outmessage

  @inline final def printWriter = printwriter

  @inline final def remaining: Int = readbuffer.remaining

  @inline final def available: Int = writebuffer.remaining

  @inline final def keepAlive = null == inmessage || inmessage.keepalive

  @inline final def encodeOnce(encoder: Encoder, length: Int) = {
    writebuffer.limit(writebuffer.position)
    writebuffer.position(writebuffer.position - length)
    encoder.encode(writebuffer)
    encoder.finish(writebuffer)
    writebuffer.position(writebuffer.limit)
    writebuffer.limit(writebuffer.capacity)
  }

  @inline final def transferFrom(source: Channel): Unit = {
    transfersource = source
    transferdestination = socketchannel
    transfercompleted = null
  }

  @inline final def transferTo(destination: Channel, length: Long, completed: A ⇒ Unit): Nothing = {
    transfersource = length match {
      case len if 0 > len ⇒
        import aio.conduits._
        //  TarArchiveConduit(GzipConduit(ChunkedConduit(socketchannel)))
        GzipConduit(ChunkedConduit(socketchannel))
      //        DeflateConduit(ChunkedConduit(channel))
      // ChunkedConduit(socketchannel)
      case _ ⇒
        FixedLengthConduit(socketchannel, readbuffer.remaining, length)
    }
    transferdestination = destination
    transfercompleted = completed
    throw ExchangeControl
  }

  @inline final def apply(input: Input[ExchangeIo[A]], flip: Boolean): (ExchangeIteratee[A], Input[ExchangeIo[A]]) = input match {
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

  @inline final def cache(cachediteratee: Done[ExchangeIo[A], _]): Unit = {
    if (0 < readbuffer.position && 0 < readbuffer.remaining && null == cachedarray) {
      var len = readbuffer.position
      readbuffer.rewind
      len -= readbuffer.position
      if (0 < len) {
        setCachedArray(len)
        readbuffer.get(cachedarray)
        this.cachediteratee = cachediteratee
      }
    }
  }

  /**
   * Aio methods.
   */

  @inline final def read(handler: ExchangeHandler[A]) = {
    if (0 < (readbuffer.capacity - readbuffer.limit) && 0 == readbuffer.position) {
      readbuffer.position(readbuffer.limit)
      readbuffer.limit(readbuffer.capacity)
    } else {
      readbuffer.clear
    }
    socketchannel.read(readbuffer, this, handler)
  }

  @inline final def write(handler: ExchangeHandler[A], flip: Boolean) = {
    if (flip) writebuffer.flip
    socketchannel.write(writebuffer, this, handler)
  }

  @inline final def readDecoding(handler: ExchangeHandler[A]) = {
    transferbuffer.clear
    transfersource.read(transferbuffer, this, handler)
  }

  @inline final def writeDecoding(handler: ExchangeHandler[A], flip: Boolean) = {
    if (flip) transferbuffer.flip
    transferdestination.write(transferbuffer, this, handler)
  }

  @inline final def readEncoding(handler: ExchangeHandler[A]) = {
    writebuffer.clear
    writebuffer.limit(writebuffer.limit - encodingSpareBufferSize)
    transfersource.read(writebuffer, this, handler)
  }

  @inline final def writeEncoding(handler: ExchangeHandler[A], flip: Boolean, encode: Int) = {
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

  @inline final def transferClose = {
    if (null != transfercompleted) transfercompleted(attachment.get)
    transfersource.close
    transferdestination.close
    transfersource = null
    transferdestination = null
    transfercompleted = null
    writebuffer.clear
    currentiteratee = Done[ExchangeIo[A], Option[Nothing]](None)
  }

  @inline final def hasError = currentiteratee.isInstanceOf[Error[_]]

  /**
   * Internals.
   */

  @inline private[aio] final def close = keepalive = false

  @inline private[aio] final def reset = {
    readbuffer.clear
    writebuffer.clear
    currentiteratee = readiteratee
  }

  @inline private[aio] final def release(e: Throwable) = if (released.compareAndSet(false, true)) {
    e match {
      case null ⇒
      case _: java.io.IOException ⇒
      case _: java.lang.IllegalStateException ⇒ warn("release: " + e)
      case _ ⇒ error("release: " + e)
    }
    releaseByteBuffer(readbuffer)
    releaseByteBuffer(writebuffer)
    if (socketchannel.isOpen) socketchannel.asInstanceOf[SocketChannelConduit].doClose
  }

  @inline private[this] final def setCachedArray(length: Int) = if (0 < length) {
    cachedarray = new Array[Byte](length)
    peekarray = new Array[Byte](length)
  } else {
    cachediteratee = null
    cachedarray = null
    peekarray = null
  }

  protected[this] val socketchannel: SocketChannelConduit

  protected[this] val readiteratee: ExchangeIteratee[A]

  protected[this] val readbuffer: ByteBuffer

  protected[this] val writebuffer: ByteBuffer

  private[this] val transferbuffer = ByteBuffer.wrap(new Array[Byte](defaultBufferSize))

  private[this] final var released = new AtomicBoolean(false)

  private[this] final var keepalive = true

  private[this] final var currentiteratee: ExchangeIteratee[A] = readiteratee

  private[this] final var innerattachment: Option[A] = None

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

