package com.ibm

package plain

package aio

import java.nio.ByteBuffer

/**
 *
 */
trait Encoder {

  def name: String

  def encode(buffer: ByteBuffer)

  def finish(buffer: ByteBuffer)

}

