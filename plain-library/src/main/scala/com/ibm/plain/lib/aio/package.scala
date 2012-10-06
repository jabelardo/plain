package com.ibm.plain

package lib

import java.nio.channels.AsynchronousFileChannel

import language.implicitConversions

import config.CheckedConfig

package object aio

  extends CheckedConfig {

  import config._
  import config.settings._

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
  final val defaultBufferSize = getBytes("plain.io.default-buffersize", 2 * 1024).toInt

}
