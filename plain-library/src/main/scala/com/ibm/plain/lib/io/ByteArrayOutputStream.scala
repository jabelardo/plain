package com.ibm.plain

package lib

package io

import java.io.OutputStream

/**
 * Like [[java.io.ByteArrayOutputStream]], but with an external bytearray. For performance reasons it does no testing of array bounds. Check capacity to protect against overflow.
 */

class ByteArrayOutputStream(
  private[this] final val array: Array[Byte],
  private[this] final val offset: Int,
  private[this] final val length: Int)

  extends OutputStream {

  def this(arr: Array[Byte]) = this(arr, 0, arr.length)

  override def write(i: Int) = {
    array.update(position, i.toByte)
    position += 1
  }

  override def write(a: Array[Byte]) = write(a, 0, a.length)

  override def write(a: Array[Byte], offset: Int, length: Int) = {
    Array.copy(a, offset, array, position, length)
    position += length
  }

  override def flush = {}

  def getPosition = position

  def reset = { position = offset }

  /**
   * Use this method to check if the internal capacity fits the next write.
   */
  def capacity = lastposition - position

  /**
   * Returns a ByteArrayInputStream using the same external bytearray. Be careful when mixing reading from this with writing to this instance.
   */
  def getInputStream = new ByteArrayInputStream(array, offset, position - offset)

  def toByteArray = array

  private[this] var position = offset

  private[this] var lastposition = offset + length

  override def toString = getClass.getName + " a.len " + array.length + ", ofs " + offset + ", len " + length + ", pos " + position + ", lpos" + lastposition

  override def close = lastposition = position

}

