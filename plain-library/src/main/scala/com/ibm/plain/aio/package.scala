package com.ibm

package plain

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel

import scala.language.implicitConversions
import scala.util.control.ControlThrowable

import config.CheckedConfig
import logging.HasLogger

package object aio

  extends CheckedConfig

  with HasLogger {

  import config._
  import config.settings._

  /**
   * This should be thrown at the end of process or similar methods when the input was processed successfully, it is used as a success indicator at some points of AIO control flow.
   */
  final case object ControlCompleted extends ControlThrowable

  final def bestFitByteBuffer(length: Int) = {
    if (length <= tinyBufferSize)
      tinyByteBuffer
    else if (length <= defaultBufferSize)
      defaultByteBuffer
    else if (length <= largeBufferSize)
      largeByteBuffer
    else if (length <= hugeBufferSize)
      hugeByteBuffer
    else {
      warning("Allocating a ByteBuffer on the heap with a size larger then 'hugeBufferSize': " + length + " bytes")
      ByteBuffer.allocate(length)
    }
  }

  @inline def defaultByteBuffer = Aio.defaultBufferPool.getBuffer

  @inline def tinyByteBuffer = Aio.tinyBufferPool.getBuffer

  @inline def largeByteBuffer = Aio.largeBufferPool.getBuffer

  @inline def hugeByteBuffer = Aio.hugeBufferPool.getBuffer

  /**
   * Quite dangerous, never call this function on a buffer more than once or it could be later used by more than one at the same time.
   */
  final def releaseByteBuffer(buffer: ByteBuffer) = buffer.capacity match {
    case `tinyBufferSize` ⇒ Aio.tinyBufferPool.releaseBuffer(buffer)
    case `defaultBufferSize` ⇒ Aio.defaultBufferPool.releaseBuffer(buffer)
    case `largeBufferSize` ⇒ Aio.largeBufferPool.releaseBuffer(buffer)
    case `hugeBufferSize` ⇒ Aio.hugeBufferPool.releaseBuffer(buffer)
    case _ ⇒
  }

  final def format(buffer: ByteBuffer, hexdump: Boolean = true) = "ByteBuffer(" + System.identityHashCode(buffer) + ", pos " + buffer.position + ", remain " + buffer.remaining + ", lim " + buffer.limit + ", cap " + buffer.capacity + ", " + (if (buffer.hasArray) "heap" else "direct") + ")" + (if (hexdump) "\n" + text.hexDump(buffer) else "")

  final val FutureNotSupported = new UnsupportedOperationException("Future not supported.")

  final val tooTinyToCareSize = getBytes("plain.aio.too-tiny-to-care-size", 1024).toInt

  /**
   * If not set differently this will result to 54k which proved to provide best performance under high load.
   */
  final val defaultBufferSize = getBytes("plain.aio.default-buffer-size", 54 * 1024).toInt

  final val defaultBufferPoolSize = getBytes("plain.aio.default-buffer-pool-size", 10000).toInt

  final val tinyBufferSize = getBytes("plain.aio.tiny-buffer-size", 4 * 1024).toInt

  final val tinyBufferPoolSize = getBytes("plain.aio.tiny-buffer-pool-size", 10000).toInt

  /**
   * Should be large enough to make an SSL packet fit into it.
   */
  final val largeBufferSize = getBytes("plain.aio.large-buffer-size", 64 * 1024).toInt

  final val largeBufferPoolSize = getBytes("plain.aio.large-buffer-pool-size", 1000).toInt

  /**
   * Used for huge entities that are handled at once.
   */
  final val hugeBufferSize = getBytes("plain.aio.huge-buffer-size", 128 * 1024).toInt

  final val hugeBufferPoolSize = getBytes("plain.aio.huge-buffer-pool-size", 1000).toInt

  /**
   *
   */
  final val readWriteTimeout = getMilliseconds("plain.aio.read-write-timeout", 5000)

  /**
   * Set on accept and connection socket for send and receive buffer size. Larger sizes perform better in LAN and fast intranets, but may waste bandwidth. Play with it.
   */
  final val sendReceiveBufferSize = getBytes("plain.aio.send-receive-buffer-size", 54 * 1024).toInt

  final val tcpNoDelay = getInt("plain.aio.tcp-no-delay", 0)

  final val maxRoundTrips = getInt("plain.aio.max-roundtrips", 1000)

  /**
   * Check requirements.
   */
  require(16 <= defaultBufferSize, "plain.aio.default-buffer-size must be >= " + 16)

  require(16 <= tinyBufferSize, "plain.aio.tiny-buffer-size must be >= " + 16)

  require(2 * 1024 <= largeBufferSize, "plain.aio.large-buffer-size must be >= " + 2 * 1024)

  require(64 * 1024 <= hugeBufferSize, "plain.aio.huge-buffer-size must be >= " + 64 * 1024)

  require((tinyBufferSize <= defaultBufferSize) && (defaultBufferSize <= largeBufferSize) && (largeBufferSize <= hugeBufferSize), "plain.aio buffer sizes: tiny <= default <= large <= huge violated")

}
