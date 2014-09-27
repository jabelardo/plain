package com.ibm

package plain

package io

import java.io.{ InputStream, OutputStream }

import net.jpountz.lz4.{ LZ4BlockInputStream, LZ4BlockOutputStream, LZ4Factory }
import os._

/**
 *
 */
object LZ4 {

  def fastOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, defaultLargeBufferSize, factory.fastCompressor)

  def highOutputStream(out: OutputStream): OutputStream = new LZ4BlockOutputStream(out, defaultLargeBufferSize, factory.highCompressor)

  def inputStream(in: InputStream): InputStream = new LZ4BlockInputStream(in, factory.fastDecompressor)

  /**
   * Fast instance based on JNI is not ported to hpux.
   */
  private[this] final val factory = operatingSystem match {
    case OperatingSystem.HPUX ⇒ LZ4Factory.unsafeInstance
    case _ ⇒ LZ4Factory.fastestInstance
  }

}
