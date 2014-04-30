package com.ibm

package plain

package aio

import java.net.StandardSocketOptions.{ SO_KEEPALIVE, SO_RCVBUF, SO_REUSEADDR, SO_SNDBUF, SO_LINGER, TCP_NODELAY }
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler }
import java.util.concurrent.TimeUnit

/**
 *
 */
final class AsynchronousSocketChannelWithTimeout private (

  final val channel: SocketChannel)

  extends Channel {

  /**
   * Call doClose if you really want to close the underlying channel.
   */
  @inline final def doClose = channel.close

  /**
   * close is ignored to avoid premature closing after reading an entity. Call doClose instead.
   */
  @inline final def close = ()

  @inline final def isOpen = channel.isOpen

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.read(buffer, 500, TimeUnit.MILLISECONDS, attachment, handler)
  }

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.write(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

  // must not be used 

  final def read(buffer: ByteBuffer) = unsupported

  final def write(buffer: ByteBuffer) = unsupported

}

object AsynchronousSocketChannelWithTimeout {

  final def apply(channel: SocketChannel) = new AsynchronousSocketChannelWithTimeout(tweak(channel))

  @inline private[this] final def tweak(channel: SocketChannel): SocketChannel = {
    tcpNoDelay match {
      case 1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(true))
      case -1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(false))
      case _ ⇒
    }
    channel.setOption(SO_REUSEADDR, Boolean.box(true))
    channel.setOption(SO_KEEPALIVE, Boolean.box(false))
    if (0 < sendReceiveBufferSize) {
      channel.setOption(SO_RCVBUF, Integer.valueOf(sendReceiveBufferSize))
      channel.setOption(SO_SNDBUF, Integer.valueOf(sendReceiveBufferSize))
    }
    channel
  }

}

