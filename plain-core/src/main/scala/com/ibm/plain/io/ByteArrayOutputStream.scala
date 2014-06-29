package com.ibm

package plain

package io

import java.io.OutputStream
import java.util.Arrays.copyOf

/**
 * Non-thread-safe version of java.io.ByteArrayOutputStream
 */

final class ByteArrayOutputStream(

  private[this] final var capacity: Int)

    extends OutputStream {

  override final def close = capacity = position

  override final def flush = ()

  override final def write(a: Array[Byte]) = write(a, 0, a.length)

  override final def write(a: Array[Byte], offset: Int, length: Int) = {
    ensureCapacity(position + length)
    Array.copy(a, offset, array, position, length)
    position += length
  }

  override final def write(i: Int) = {
    ensureCapacity(position + 1)
    array.update(position, i.toByte)
    position += 1
  }

  final def setCapacity(c: Int) = ensureCapacity(c - position)

  final def getCapactiy = capacity

  final def length = position

  final def reset = { position = 0; capacity = array.length }

  final def toByteArray = copyOf(array, position)

  final def getArray = array

  @inline private[this] final def ensureCapacity(c: Int) = {
    if (c > capacity) {
      while (capacity < c) capacity <<= 1
      array = copyOf(array, capacity)
    }
  }

  private[this] final var array = {
    if (!math.isPowerOfTwo(capacity)) capacity = math.nextPowerOfTwo(capacity)
    new Array[Byte](capacity)
  }

  private[this] final var position = 0

}

