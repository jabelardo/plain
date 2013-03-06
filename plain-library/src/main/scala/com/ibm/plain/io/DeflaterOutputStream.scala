package com.ibm

package plain

package aio

import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.{ DeflaterOutputStream ⇒ JDeflaterOutputStream, GZIPOutputStream ⇒ JGZIPOutputStream, Deflater, CRC32 }

/**
 *
 */
final class DeflaterOutputStream(

  output: OutputStream,

  compressionlevel: Int)

  extends JDeflaterOutputStream(output) {

  `def`.setLevel(compressionlevel)

  def this(output: OutputStream) = this(output, Deflater.BEST_SPEED)

}

/**
 * With this class it's possible to set the compression level on the GZIPOutputStream.
 */
final class GZIPOutputStream(

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
