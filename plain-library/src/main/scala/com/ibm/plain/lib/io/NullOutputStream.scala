package com.ibm.haploid

package core

package util

package io

import java.io.OutputStream

/**
 * Writing to the NullOutputStream object is like writing to /dev/null.
 * It is thread-safe. Use it if you need to write to a [[java.io.OutputStream]], but have no interest in the output.
 */
trait NullOutputStream extends OutputStream {

  override def toString = ""

  override def write(i: Int) = ()

  override def write(a: Array[Byte]) = ()

  override def write(a: Array[Byte], offset: Int, length: Int) = ()

  override def close = ()

  override def flush = ()

}

object NullOutputStream extends NullOutputStream

/**
 * Writing to the CountingNullOutputStream object is like writing to /dev/null but it keeps track of the number of bytes written to it.
 * This is not thread-safe. Use it if you need to write to a [[java.io.OutputStream]], but only need to now the size.
 */
final class CountingNullOutputStream extends OutputStream {

  override def toString = count.toString

  override def write(i: Int) = count += 4

  override def write(a: Array[Byte]) = write(a, 0, a.length)

  override def write(a: Array[Byte], offset: Int, length: Int) = count += length

  /**
   * Returns the number of bytes written so far to this stream.
   */
  def size = count

  private[this] var count = 0L

}

