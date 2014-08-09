package com.ibm

package plain

package io

import java.io.OutputStream
import java.util.zip.{ Deflater, DeflaterOutputStream ⇒ JDeflaterOutputStream }

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
