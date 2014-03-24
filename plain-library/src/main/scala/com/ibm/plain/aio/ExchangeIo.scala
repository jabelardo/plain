package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.Arrays

import scala.math.min

/**
 *
 */
final class ExchangeIo private (

  buffer: ByteBuffer) {

  import ExchangeIo._

  @inline final def ++(that: ExchangeIo): ExchangeIo = if (this eq that) that else { throw new NotImplementedError }

  final def decode(characterset: Charset, lowercase: Boolean): String = advanceBuffer(
    buffer.remaining match {
      case 0 ⇒ emptyString
      case n ⇒ readBytes(n) match {
        case a if a eq array ⇒ StringPool.get(if (lowercase) lowerAlphabet(a, 0, n) else a, n, characterset)
        case a ⇒ new String(a, 0, n, characterset)
      }
    })

  @inline final def consume: Array[Byte] = advanceBuffer(
    buffer.remaining match {
      case 0 ⇒ Io.emptyArray
      case n ⇒ readBytes(n)
    })

  @inline final def length: Int = buffer.remaining

  @inline final def take(n: Int): ExchangeIo = {
    markLimit
    buffer.limit(min(buffer.limit, buffer.position + n))
    this
  }

  @inline final def peek(n: Int): ExchangeIo = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = buffer.get(buffer.position)

  @inline final def drop(n: Int): ExchangeIo = {
    buffer.position(min(buffer.limit, buffer.position + n))
    this
  }

  @inline final def indexOf(b: Byte): Int = {
    val p = buffer.position
    val l = buffer.limit
    var i = p
    while (i < l && b != buffer.get(i)) i += 1
    if (i == l) -1 else i - p
  }

  @inline final def span(p: Int ⇒ Boolean): (Int, Int) = {
    val pos = buffer.position
    val l = buffer.limit
    var i = pos
    while (i < l && p(buffer.get(i))) i += 1
    (i - pos, l - i)
  }

  @inline final def remaining = buffer.remaining

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (n <= StringPool.maxStringLength) { buffer.get(array, 0, n); array } else Array.fill(n)(buffer.get)

  @inline private[this] final def markLimit: Unit = limitmark = buffer.limit

  @inline private[this] final def markPosition: Unit = positionmark = buffer.position

  @inline private[this] final def advanceBuffer[A](a: A): A = {
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
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
object ExchangeIo {

  final def apply(buffer: ByteBuffer): ExchangeIo = new ExchangeIo(buffer)

  final val emptyString = new String

  final val emptyArray = new Array[Byte](0)

}