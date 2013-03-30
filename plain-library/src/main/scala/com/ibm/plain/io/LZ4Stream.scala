package com.ibm

package plain

package io

import java.io.{ InputStream, OutputStream }

import net.jpountz.lz4.{ LZ4BlockInputStream, LZ4BlockOutputStream, LZ4Factory }

/**
 *
 */
object LZ4 {

  def newFastOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, 64 * 1024, factory.fastCompressor)

  def newHighOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, 64 * 1024, factory.highCompressor)

  def newInputStream(in: InputStream): InputStream = new LZ4BlockInputStream(in, factory.decompressor)

  private[this] final val factory = LZ4Factory.unsafeInstance

}
