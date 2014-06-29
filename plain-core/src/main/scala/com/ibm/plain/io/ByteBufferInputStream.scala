package com.ibm

package plain

package io

import java.io.InputStream
import java.nio.ByteBuffer

import scala.math.min

/**
 *
 */
final class ByteBufferInputStream(

  private[this] final val buffer: ByteBuffer)

    extends InputStream {

  @inline override final def close = buffer.position(buffer.limit)

  @inline override final def read: Int = {
    val b: Byte = buffer.get
    b & 0xff
  }

  @inline override final def read(a: Array[Byte]) = read(a, 0, a.length)

  @inline override final def read(a: Array[Byte], offset: Int, length: Int) = {
    val len = min(length, available)
    if (0 < len) {
      buffer.get(a, offset, len)
      len
    } else {
      -1
    }
  }

  @inline override final def available = buffer.remaining

}

