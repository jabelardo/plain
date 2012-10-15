package com.ibm.plain

package lib

package http

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousChannelGroup ⇒ Group, AsynchronousServerSocketChannel ⇒ ServerChannel }
import java.util.concurrent.TimeUnit

import scala.concurrent.util.Duration
import scala.util.continuations.{ reset, shift, suspendable }

import HttpIteratee.readRequest
import aio.{ Cont, Done, Error }
import aio.Io._
import bootstrap.BaseComponent
import logging.HasLogger

/**
 *
 */
case class HttpServer(

  port: Int,

  backlog: Int)

  extends BaseComponent[HttpServer](
    "HttpServer(address:" + new InetSocketAddress(port) + ", backlog:" + backlog + ")")

  with HasLogger {

  import HttpServer._

  override def isStarted = synchronized { null != serverChannel }

  override def start = try {
    if (isEnabled) {
      serverChannel = ServerChannel.open(channelGroup).bind(new InetSocketAddress(port), backlog)
      reset {
        handle(accept(serverChannel) ++ readRequest)
      }
      debug(name + " has started.")
    }
    this
  } catch {
    case e: Throwable ⇒ error(name + " failed to start : " + e); throw e
  }

  override def stop = try {
    if (isStarted) synchronized {
      serverChannel.close
      serverChannel = null
      debug(name + " has stopped.")
    }
    this
  } catch {
    case e: Throwable ⇒ error(name + " failed to stop : " + e); this
  }

  override def awaitTermination(timeout: Duration) = if (!channelGroup.isShutdown) channelGroup.awaitTermination(if (Duration.Inf == timeout) -1 else timeout.toMillis, TimeUnit.MILLISECONDS)

  private[this] var serverChannel: ServerChannel = null

}

/**
 * Contains common things shared among several HttpServers, if any.
 */
object HttpServer {

  /**
   * Do not call channelGroup.shutdown as it prematurely shuts down the threadpool that is shares with akka.
   */
  private final val channelGroup = Group.withThreadPool(concurrent.executor)

}

