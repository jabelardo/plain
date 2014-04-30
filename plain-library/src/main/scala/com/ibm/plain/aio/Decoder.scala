package com.ibm

package plain

package aio

import java.nio.ByteBuffer

/**
 *
 */
trait Decoder {

  def name: String

  def remaining: Int

  def decode(buffer: ByteBuffer)

  def finish(buffer: ByteBuffer)

}

