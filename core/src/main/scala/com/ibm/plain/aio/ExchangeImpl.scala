package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.nio.charset.Charset
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean

import scala.math.min

import Input.Elem
import Iteratee.{ Cont, Done, Error }
import conduit.{ ChunkedConduit, GzipConduit, SocketChannelConduit, TarConduit, TerminatingConduit }
import io.PrintWriter
import concurrent.spawn

/**
 *
 */
final class ExchangeImpl[A] private[aio] (

  protected[this] final val socketchannel: SocketChannelConduit,

  protected[this] final val readiteratee: ExchangeIteratee[A],

  protected[this] final val readbuffer: ByteBuffer,

  protected[this] final val writebuffer: ByteBuffer)

    extends Exchange[A]

    with ExchangeAccessImpl[A]

    with ExchangeIoImpl[A]

/**
 *
 */
trait ExchangeAccessImpl[A]

    extends ExchangeAccess[A]

    with ExchangeAio[A] {

  self: Exchange[A] ⇒

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

  @inline final def transferFrom(source: TerminatingConduit): Unit = {
    require(null != transferdestination, "transferdestination must be set before calling transferFrom.")
    transfersource = source
    transfercompleted = null
    transferfrom = true
  }

  @inline final def transferTo(destination: TerminatingConduit, completed: A ⇒ Unit): Nothing = {
    require(null != transfersource, "transfersource must be set before calling transferTo.")
    transferdestination = destination
    transfercompleted = completed
    transferfrom = false
    throw ExchangeControl
  }

  @inline final def apply(input: Input[ExchangeIo[A]], flip: Boolean): (ExchangeIteratee[A], Input[ExchangeIo[A]]) = {
    input match {
      case Elem(_) ⇒
        if (flip) readbuffer.flip
        val fromcache = if (null == cachedarray) {
          false
        } else if (readbuffer.remaining >= cachedarray.length) {
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
        if (fromcache) {
          (cachediteratee, Elem(this))
        } else {
          currentiteratee match {
            case Done(_) ⇒ currentiteratee = readiteratee
            case _ ⇒
          }
          currentiteratee(input)
        }
      case _ ⇒
        readbuffer.flip
        currentiteratee(input)
    }
  }

  @inline final def cache(cachediteratee: Done[ExchangeIo[A], _]): Unit = {
    if (null == cachedarray) {
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

  @inline final def readTransfer(handler: ExchangeHandler[A]) = {
    writebuffer.clear
    transfersource.read(writebuffer, this, handler)
  }

  @inline final def writeTransfer(handler: ExchangeHandler[A], flip: Boolean) = spawn {
    if (flip) writebuffer.flip
    transferdestination.write(writebuffer, this, handler)
  }

  @inline final def isTransferFrom = transferfrom

  @inline final def closeTransfer = {
    writebuffer.clear
    if (null != transfercompleted) {
      transfercompleted(attachment.get)
      transfercompleted = null
    }
    if (null != transfersource) {
      transfersource.close
      transfersource = socketchannel
    }
    if (null != transferdestination) {
      transferdestination.close
      transferdestination = socketchannel
    }
    currentiteratee = if (transferfrom) readiteratee else Done[ExchangeIo[A], Null](null)
  }

  @inline private[plain] def setDestination(destination: Channel) = {
    transferdestination = destination
  }

  @inline private[plain] def setSource(source: Channel) = {
    transfersource = source
  }

  @inline final def hasError = currentiteratee.isInstanceOf[Error[_]]

  @inline private[aio] final def close = keepalive = false

  @inline private[aio] final def reset = {
    readbuffer.clear
    writebuffer.clear
    currentiteratee = readiteratee
  }

  @inline private[aio] final def release(e: Throwable) = {
    if (released.compareAndSet(false, true)) {
      releaseByteBuffer(readbuffer)
      releaseByteBuffer(writebuffer)
      if (socketchannel.isOpen) socketchannel.asInstanceOf[SocketChannelConduit].doClose
      e match {
        case null ⇒
        case _: java.io.IOException ⇒
        case _: java.lang.IllegalStateException ⇒ Exchange.warn("release: " + e)
        case _ ⇒ Exchange.error("release: " + e)
      }
    }
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

  private[this] final var transfersource: Channel = socketchannel

  private[this] final var transferdestination: Channel = socketchannel

  private[this] final var transfercompleted: A ⇒ Unit = null

  private[this] final var transferfrom: Boolean = false

}

/**
 *
 */
private[aio] trait ExchangeIoImpl[A]

    extends ExchangeIo[A] {

  import ExchangeIoImpl._

  final def length = readbuffer.remaining

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

  @inline final def take(n: Int): ExchangeIo[A] = {
    markLimit
    readbuffer.limit(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  @inline final def peek(n: Int): ExchangeIo[A] = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = readbuffer.get(readbuffer.position)

  @inline final def drop(n: Int): ExchangeIo[A] = {
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

  @inline final def ++(that: ExchangeIo[A]): ExchangeIo[A] = {
    if (this eq that) that else unsupported
  }

  private[this] final def readBytes(n: Int): Array[Byte] = if (n <= StringPool.maxStringLength) { readbuffer.get(array, 0, n); array } else Array.fill(n)(readbuffer.get)

  @inline private[this] final def markLimit: Unit = limitmark = readbuffer.limit

  @inline private[this] final def markPosition: Unit = positionmark = readbuffer.position

  private[this] final def advanceBuffer[WhatEver](whatever: WhatEver): WhatEver = {
    readbuffer.limit(limitmark)
    if (-1 < positionmark) { readbuffer.position(positionmark); positionmark = -1 }
    whatever
  }

  private[this] final def lowerAlphabet(a: Array[Byte], offset: Int, length: Int): Array[Byte] = {
    for (i ← offset until length) { val b = a(i); if ('A' <= b && b <= 'Z') a.update(i, (b + 32).toByte) }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

  private[this] final val array = new Array[Byte](StringPool.maxStringLength)

  protected[this] val readbuffer: ByteBuffer

  protected[this] val writebuffer: ByteBuffer

}

/**
 * Some statics put here.
 */
private object ExchangeIoImpl {

  /**
   * Constants.
   */
  private final val emptyString = new String

  private final val emptyArray = new Array[Byte](0)

}

