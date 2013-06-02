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
private final class SocketChannelWithTimeout private (

  private[this] final val channel: SocketChannel)

  extends Channel {

  @inline final def close = channel.close

  @inline final def isOpen = channel.isOpen

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.read(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.write(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

  final def read(buffer: ByteBuffer) = channel.read(buffer)

  final def write(buffer: ByteBuffer) = channel.write(buffer)

}

private object SocketChannelWithTimeout {

  final def apply(channel: SocketChannel) = new SocketChannelWithTimeout(tweak(channel))

  @inline private[this] final def tweak(channel: SocketChannel): SocketChannel = {
    tcpNoDelay match {
      case 1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(true))
      case -1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(false))
      case _ ⇒
    }
    channel.setOption(SO_REUSEADDR, Boolean.box(true))
    channel.setOption(SO_KEEPALIVE, Boolean.box(false))
    channel.setOption(SO_RCVBUF, Integer.valueOf(sendReceiveBufferSize))
    channel.setOption(SO_SNDBUF, Integer.valueOf(sendReceiveBufferSize))
  }

}

