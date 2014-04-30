package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.util.zip.Inflater

import aio.Decoder

/**
 *
 */
final class GzipDecoder private

  extends BaseDecoder(true) {

  final def name = "gzip"

  final def remaining = -1

  override final def decode(buffer: ByteBuffer) = {}

}

object GzipDecoder {

  def apply = new GzipDecoder

}

