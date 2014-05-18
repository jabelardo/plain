package com.ibm

package plain

package io

import java.io.OutputStream
import java.nio.ByteBuffer

import scala.math.min

/**
 *
 */
final class ByteBufferOutputStream(

  private[this] final val buffer: ByteBuffer)

  extends OutputStream {

  @inline override final def close = {
    buffer.position(buffer.limit)
  }

  @inline override final def flush = ()

  @inline override final def write(i: Int) = {
    buffer.put(i.toByte)
  }

  @inline override final def write(a: Array[Byte]) = write(a, 0, a.length)

  @inline override final def write(a: Array[Byte], offset: Int, length: Int) = {
    buffer.put(a, offset, min(length, buffer.remaining))
  }

}

