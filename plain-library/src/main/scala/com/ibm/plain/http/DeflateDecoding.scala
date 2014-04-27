package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.Inflater

import aio.Decoder

/**
 *
 */
abstract class BaseDecoder

  extends Decoder {

  final def finish(buffer: ByteBuffer) = {

  }

}

/**
 * Prefer "deflate" over "gzip", depending on input it can be more than 100% faster.
 */
final class DeflateDecoder private

  extends BaseDecoder {

  final def name = "deflate"

  final def decode(buffer: ByteBuffer) = {
    println("in decode " + aio.format(buffer))
  }

}

object DeflateDecoder {

  def apply = new DeflateDecoder

}

