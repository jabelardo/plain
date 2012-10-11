package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.nio.charset.Charset

import scala.annotation.tailrec

import text.{ ASCII, UTF8 }

/**
 * ByteBufferInput is Input of type ByteBuffer and is consumed by an Iteratee[ByteBufferInput, A] to produce a result of A.
 *
 * This implementation assumes that the input ByteBuffer is almost always of sufficient size to contain
 * all that's necessary to produce an A. Very (!) rarely it might need to call ++ to append more input.
 * It also aims to be very fast and therefore allows it to be mutable. Another basic assumption is that ByteBufferInput is
 * read from left to right with very few look-ahead.
 */
abstract class ByteBufferInput(

  protected val in: ByteBuffer)

  extends Input[ByteBuffer] {

  /**
   * Do not call "relative" get or put methods on in: ByteBuffer for they would change it's internal state.
   */
  import ByteBufferInput._

  override def toString = getClass.getSimpleName + "(" + in + " pos=" + position + " lim=" + limit + " rem=" + remaining + " len " + length + " mar=" + mark + ")"

  def ++(that: ByteBufferInput): ByteBufferInput = that

  final def length = limit - position

  final def remaining = in.limit - limit

  def take(n: Int): ByteBufferInput = {
    limit = position + n
    this
  }

  def takeWhile(p: Int ⇒ Boolean): ByteBufferInput = {
    var pos = position
    while (pos < limit && p(in.get(pos))) pos += 1
    limit = pos
    this
  }

  def takeUntil(delimiter: Byte): ByteBufferInput = takeWhile(_ != delimiter)

  /**
   * This is mainly implemented for `\r\n`.
   */
  def takeUntil(delimiter: Array[Byte]): ByteBufferInput = {
    require(3 > delimiter.length, "Only implemented for delimiter.length < 3")
    delimiter.length match {
      case 0 ⇒ limit = position
      case 1 ⇒ takeWhile(_ != delimiter(0))
      case 2 ⇒
        var pos = position
        val a = delimiter(0)
        val b = delimiter(1)
        while (pos + 1 < limit && a != in.get(pos) && b != in.get(pos + 1)) pos += 1
        limit = pos
    }
    this
  }

  def peek(n: Int): ByteBufferInput = {
    mark = position
    limit = position + n
    this
  }

  def drop(n: Int): ByteBufferInput = {
    position += n
    this
  }

  def dropWhile(p: Byte ⇒ Boolean): ByteBufferInput = {
    while (p(in.get(position)) && position < limit) position += 1
    this
  }

  def decode(cset: Charset): String = reset(new String(readBytes, cset))

  def reset = in.reset

  private def readBytes: Array[Byte] = Array.fill(length)(readByte)

  private[this] def readByte: Byte = in.get(getAndIncrement)

  @inline private def reset[T](t: T): T = { limit = in.limit; if (-1 < mark) { position = mark; mark = -1 }; t }

  @inline private[this] def getAndIncrement = {
    val p = position
    position += 1
    p
  }

  private[this] var limit = in.limit

  private[this] var position = in.position

  private[this] var mark = -1

}

object ByteBufferInput

  extends logging.HasLogger {

  def apply(buffer: ByteBuffer): ByteBufferInput = new ByteBufferInput(buffer) {

    override def ++(that: ByteBufferInput): ByteBufferInput = if (0 == this.length)
      that
    else if (0 == that.length)
      this
    else {
      warning("Avoid this by enlarging buffersize " + this + " " + that)
      val b = ByteBuffer.allocate(this.length + that.length)
      b.put(this.reset(this.readBytes))
      b.put(that.reset(that.readBytes))
      b.flip
      ByteBufferInput(b)
    }

  }

  final val empty = apply(ByteBuffer.wrap(Array.empty))

}

