package com.ibm

package plain

package http

import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.Inflater

import scala.math.min

import aio.Decoder

/**
 *
 */
abstract class BaseDecoder(

  nowrap: Boolean)

  extends Decoder {

  final def finish(buffer: ByteBuffer) = ()

  def decode(buffer: ByteBuffer) = if (null == in) {
    in = new Array[Byte](buffer.capacity)
    out = new Array[Byte](buffer.capacity)
    firstChunk(buffer)
  }

  protected[this] final def firstChunk(buffer: ByteBuffer) = {
    inposition = buffer.position
    inremaining = buffer.remaining
    buffer.get(in, 0, buffer.remaining)
    "[0-9a-fA-F]+\r\n".r.findFirstIn(new String(in, inposition, min(inremaining, 12))) match {
      case Some(header) ⇒ setChunk(Integer.parseInt(header.dropRight(2), 16), header.length)
      case _ ⇒ throw new IOException
    }
    buffer.clear
    println("first " + this)
  }

  protected[this] final def nextChunk(buffer: ByteBuffer) = {
    "\r\n[0-9a-fA-F]+\r\n".r.findFirstIn(new String(in, inposition, min(inremaining, 12))) match {
      case Some(header) ⇒ setChunk(Integer.parseInt(header.drop(2).dropRight(2), 16), header.length)
      case _ ⇒ println("error #1"); throw new IOException
    }
    println("next " + this)
  }

  protected[this] final val inflater = new Inflater(nowrap)

  protected[this] final var in: Array[Byte] = null

  protected[this] final var out: Array[Byte] = null

  protected[this] final var inposition: Int = -1

  protected[this] final var inremaining: Int = -1

  protected[this] final var chunkposition: Int = -1

  protected[this] final var chunkremaining: Int = -1

  private[this] final def setChunk(chunklen: Int, headerlen: Int) = {
    println("chunk " + chunklen)
    inposition += headerlen
    inremaining -= headerlen
    chunkposition = inposition
    chunkremaining = min(chunklen, in.length - chunkposition)
    if (chunklen > chunkremaining) println("warn: end of chunk not in readbuffer")
    inposition += chunkremaining
    inremaining -= chunkremaining
    inflater.setInput(in, chunkposition, chunkremaining)
  }

  override def toString = "Decoder " +
    inposition + " " +
    inremaining + " " +
    chunkposition + " " +
    chunkremaining + " " +
    inflater.getRemaining + " " +
    inflater.needsInput + " " +
    inflater.getTotalIn + " " +
    inflater.getTotalOut

}

/**
 * Prefer "deflate" over "gzip", depending on input it can be more than 100% faster.
 */
final class DeflateDecoder private

  extends BaseDecoder(false) {

  final def name = "deflate"

  var c = 0

  final def remaining = bytesinflated

  override final def decode(buffer: ByteBuffer) = {
    super.decode(buffer)
    c += 1
    println("c" + c)
    if (0 < inflater.getRemaining) {
      bytesinflated = inflater.inflate(out, 0, out.length)
      println("before " + buffer)
      buffer.put(out, 0, bytesinflated)
      println("after " + this + " " + " " + bytesinflated + " " + buffer)
    } else if (inflater.needsInput && 0 < inremaining) {
      println("need next chunk " + this)
      nextChunk(buffer)
      decode(buffer)
    } else {
      bytesinflated = 0
    }
  }

  private[this] final var bytesinflated = 0

}

object DeflateDecoder {

  def apply = new DeflateDecoder

}

