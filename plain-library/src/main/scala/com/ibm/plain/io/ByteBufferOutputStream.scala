package com.ibm

package plain

package io

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 *
 */
final class ByteBufferOutputStream(

  private[this] final val buffer: ByteBuffer)

  extends OutputStream {

  @inline override final def close = {
    buffer.limit(position)
    buffer.position(0)
  }

  @inline override final def flush = ()

  @inline override final def write(i: Int) = {
    buffer.limit(limit)
    buffer.put(position, i.toByte)
    position += 1
  }

  @inline override final def write(a: Array[Byte]) = write(a, 0, a.length)

  @inline override final def write(a: Array[Byte], offset: Int, length: Int) = {
    buffer.limit(limit)
    buffer.position(position)
    buffer.put(a, offset, length)
    position += length
  }

  private[this] final var position = 0

  private[this] final val limit = buffer.capacity

}

