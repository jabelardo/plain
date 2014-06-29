package com.ibm

package plain

package io

import java.io.OutputStream
import java.util.zip.{ Deflater, GZIPOutputStream ⇒ JGZIPOutputStream }

/**
 * With this class it's possible to set the compression level on the GzipOutputStream.
 */
final class GzipOutputStream(

  output: OutputStream,

  buffersize: Int,

  syncmode: Boolean,

  compressionlevel: Int)

  extends JGZIPOutputStream(output, buffersize, syncmode) {

  `def`.setLevel(compressionlevel)

  def this(output: OutputStream) = this(output, defaultBufferSize, false, Deflater.BEST_SPEED)

  def this(output: OutputStream, compressionlevel: Int) = this(output, defaultBufferSize, false, compressionlevel)

  def this(output: OutputStream, buffersize: Int, syncmode: Boolean) = this(output, buffersize, syncmode, Deflater.DEFAULT_COMPRESSION)

}
