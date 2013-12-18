package com.ibm

package plain

package io

import java.io.OutputStream
import java.util.Arrays.copyOf

/**
 * Non-thread-safe version of java.io.ByteArrayOutputStream
 */

final class ByteArrayOutputStream private (

  private[this] final var capacity: Int)

  extends OutputStream {

  override final def close = capacity = position

  override final def flush = ()

  override final def write(a: Array[Byte], offset: Int, length: Int) = {
    ensureCapacity(length)
    Array.copy(a, offset, array, position, length)
    position += length
  }

  override final def write(a: Array[Byte]) = write(a, 0, a.length)

  override final def write(i: Int) = {
    ensureCapacity(1)
    array.update(position, i.toByte)
    position += 1
  }

  final def setCapacity(cap: Int) = ensureCapacity(cap - position)

  final def getCapactiy = capacity

  final def size = position

  final def reset = { position = 0; capacity = array.length }

  final def toByteArray = copyOf(array, position)

  @inline private[this] def ensureCapacity(cap: Int) = {
    if (cap > capacity - position) {
      capacity <<= 1
      array = copyOf(array, capacity)
    }
  }

  private[this] final var array = new Array[Byte](capacity)

  private[this] final var position = 0

}

/**
 *
 */
object ByteArrayOutputStream {

  final def apply(capacity: Int) = new ByteArrayOutputStream(capacity)

}