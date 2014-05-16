package com.ibm

package plain

package io

import java.io.InputStream
import java.nio.ByteBuffer

/**
 *
 */
final class ByteBufferInputStream(

  private[this] final val buffer: ByteBuffer)

  extends InputStream {

  @inline override final def close = position = limit

  @inline override final def read: Int = {
    buffer.limit(limit)
    val b = buffer.get(position)
    position += 1
    b & 0xff
  }

  @inline override final def read(a: Array[Byte]) = read(a, 0, a.length)

  @inline override final def read(a: Array[Byte], offset: Int, length: Int) = {
    buffer.limit(limit)
    buffer.position(position)
    val len = scala.math.min(length, limit - position)
    if (0 < len) {
      buffer.get(a, offset, len)
      position += len
      len
    } else {
      -1
    }
  }

  @inline override final def available = limit - position

  private[this] final var position = buffer.position

  private[this] final val limit = buffer.limit

}

