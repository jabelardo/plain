package com.ibm

package plain

package io

import java.io.OutputStream

/**
 * Like java.io.ByteArrayOutputStream, but with an external bytearray. For performance reasons it does no testing of array bounds. Check capacity to protect against overflow.
 */

final class ByteArrayOutputStream private (

  private[this] final val array: Array[Byte],

  private[this] final val offset: Int,

  private[this] final val length: Int)

  extends OutputStream {

  def this(arr: Array[Byte]) = this(arr, 0, arr.length)

  override final def write(i: Int) = {
    array.update(position, i.toByte)
    position += 1
  }

  override final def write(a: Array[Byte]) = write(a, 0, a.length)

  override final def write(a: Array[Byte], offset: Int, length: Int) = {
    Array.copy(a, offset, array, position, length)
    position += length
  }

  override final def flush = {}

  final def getPosition = position

  final def reset = { position = offset }

  /**
   * Use this method to check if the internal capacity fits the next write.
   */
  final def capacity = lastposition - position

  /**
   * Returns a ByteArrayInputStream using the same external bytearray. Be careful when mixing reading from this with writing to this instance.
   */
  final def getInputStream = new ByteArrayInputStream(array, offset, position - offset)

  final def toByteArray = {
    val a = new Array[Byte](position)
    Array.copy(array, 0, a, 0, position)
    a
  }

  private[this] var position = offset

  private[this] var lastposition = offset + length

  override final def toString = getClass.getName + " a.len " + array.length + ", ofs " + offset + ", len " + length + ", pos " + position + ", lpos" + lastposition

  final override def close = lastposition = position

}

/**
 *
 */
object ByteArrayOutputStream {

  final def apply(array: Array[Byte], offset: Int, length: Int) = new ByteArrayOutputStream(array, offset, length)

  final def apply(array: Array[Byte]) = new ByteArrayOutputStream(array, 0, array.length)

}