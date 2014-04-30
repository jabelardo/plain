package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.charset.Charset

import scala.math.min

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
 * Some statics put here.
 */
private object ExchangeIo {

  /**
   * Constants.
   */
  final val emptyString = new String

  final val emptyArray = new Array[Byte](0)

}

/**
 *
 */
private[aio] trait ExchangeIoImpl[A]

  extends ExchangeIo[A] {

  import ExchangeIo._

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
