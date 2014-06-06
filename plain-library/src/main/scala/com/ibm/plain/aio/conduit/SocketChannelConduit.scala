package com.ibm

package plain

package aio

package conduit

import java.net.StandardSocketOptions.{ SO_KEEPALIVE, SO_RCVBUF, SO_REUSEADDR, SO_SNDBUF, TCP_NODELAY }
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousSocketChannel ⇒ SocketChannel }
import java.util.concurrent.TimeUnit

import io.IgnoringCloseable

/**
 *
 */
final class SocketChannelConduit private (

  protected[this] final val underlyingchannel: SocketChannel)

  extends SocketChannelSourceConduit

  with SocketChannelSinkConduit

  with IgnoringCloseable {

  override final def doClose = underlyingchannel.close

  final def socketChannel = underlyingchannel

}

/**
 *
 */
object SocketChannelConduit {

  final def apply(underlyingchannel: SocketChannel) = new SocketChannelConduit(tweakChannel(underlyingchannel))

  private[this] final def tweakChannel(channel: SocketChannel): SocketChannel = {
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

/**
 * Source conduit.
 */
sealed trait SocketChannelSourceConduit

  extends ConnectorSourceConduit[SocketChannel] {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    underlyingchannel.read(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

}

/**
 * Sink conduit.
 */
sealed trait SocketChannelSinkConduit

  extends ConnectorSinkConduit[SocketChannel] {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    underlyingchannel.write(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

}

