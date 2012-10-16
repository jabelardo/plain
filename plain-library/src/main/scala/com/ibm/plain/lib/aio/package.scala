package com.ibm.plain

package lib

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

import language.implicitConversions

import config.CheckedConfig

package object aio

  extends CheckedConfig {

  import config._
  import config.settings._

  def bestFitByteBuffer(length: Int) = {
    if (length <= tinyBufferSize)
      tinyByteBuffer
    else if (length <= defaultBufferSize)
      defaultByteBuffer
    else if (length <= largeBufferSize)
      largeByteBuffer
    else
      ByteBuffer.allocate(length)
  }

  def defaultByteBuffer = Aio.defaultBufferPool.getBuffer

  def tinyByteBuffer = Aio.tinyBufferPool.getBuffer

  def largeByteBuffer = Aio.largeBufferPool.getBuffer

  def releaseByteBuffer(buffer: ByteBuffer) = buffer.capacity match {
    case `tinyBufferSize` ⇒ Aio.tinyBufferPool.releaseBuffer(buffer)
    case `defaultBufferSize` ⇒ Aio.defaultBufferPool.releaseBuffer(buffer)
    case `largeBufferSize` ⇒ Aio.largeBufferPool.releaseBuffer(buffer)
    case _ ⇒
  }

  /**
   * Shorthand to object AsynchronousChannelTransfer.
   */
  final val transfer = AsynchronousChannelTransfer

  /**
   * Helper to use AsynchronousFileChannel directly for transfer.
   */
  implicit def filechannel2filebytechannel(filechannel: AsynchronousFileChannel) = AsynchronousFileByteChannel.wrap(filechannel)

  /**
   * If not set differently this will result to 2k which proved to provide best performance under high load.
   */
  final val defaultBufferSize = getBytes("plain.aio.default-buffer-size", 2 * 1024).toInt

  final val defaultBufferPoolSize = getInt("plain.aio.default-buffer-pool-size", 512)

  /**
   * If not set differently this will result to 2k which proved to provide best performance under high load.
   */
  final val tinyBufferSize = getBytes("plain.aio.tiny-buffer-size", 128).toInt

  final val tinyBufferPoolSize = getInt("plain.aio.tiny-buffer-pool-size", 1024)

  /**
   * Should be large enough to make an SSL packet fit into it.
   */
  final val largeBufferSize = getBytes("plain.aio.large-buffer-size", 20 * 1024).toInt

  final val largeBufferPoolSize = getInt("plain.aio.large-buffer-pool-size", 64)

  /**
   * Check requirements.
   */
  require(1 <= defaultBufferSize, "plain.aio.default-buffer-size must be >= " + 16)

  require(16 <= tinyBufferSize, "plain.aio.tiny-buffer-size must be >= " + 16)

  require(2 * 1024 <= largeBufferSize, "plain.aio.large-buffer-size must be >= " + 2 * 1024)

  //  require((tinyBufferSize <= defaultBufferSize) && (defaultBufferSize <= largeBufferSize), "plain.aio buffer sizes: tiny <= default <= large violated")

}
