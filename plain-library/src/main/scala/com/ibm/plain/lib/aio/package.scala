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

  def defaultByteBuffer = Aio.defaultBufferPool.getBuffer

  def largeByteBuffer = Aio.largeBufferPool.getBuffer

  def releaseByteBuffer(buffer: ByteBuffer) = buffer.capacity match {
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
   * Should be large enough to make an SSL packet fit into it.
   */
  final val largeBufferSize = getBytes("plain.aio.large-buffer-size", 20 * 1024).toInt

  final val largeBufferPoolSize = getInt("plain.aio.large-buffer-pool-size", 64)

  require(1 <= defaultBufferSize, "plain.aio.default-buffer-size must be >= " + 16)

  require(2 * 1024 <= largeBufferSize, "plain.aio.large-buffer-size must be >= " + 2 * 1024)

}
