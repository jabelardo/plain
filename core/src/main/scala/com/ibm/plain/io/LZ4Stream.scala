package com.ibm

package plain

package io

import java.io.{ InputStream, OutputStream }

import net.jpountz.lz4.{ LZ4BlockInputStream, LZ4BlockOutputStream, LZ4Factory }

/**
 *
 */
object LZ4 {

  def fastOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, defaultLargeBufferSize, factory.fastCompressor)

  def highOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, defaultLargeBufferSize, factory.highCompressor)

  def inputStream(in: InputStream): InputStream = new LZ4BlockInputStream(in, factory.fastDecompressor)

  private[this] final val factory = LZ4Factory.fastestInstance

}
