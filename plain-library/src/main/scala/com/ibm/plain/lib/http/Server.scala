package com.ibm.plain

package lib

package http

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ AsynchronousChannelGroup ⇒ Group, AsynchronousServerSocketChannel ⇒ ServerChannel }
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import scala.concurrent.util.Duration
import scala.util.continuations.reset

import com.typesafe.config.{ Config, ConfigFactory }

import aio.Io.{ accept, handle }
import bootstrap.BaseComponent
import logging.HasLogger
import config.CheckedConfig

/**
 *
 */
case class Server(

  private val path: String)

  extends BaseComponent[Server](null)

  with HasLogger {

  import Server._

  override def isStarted = synchronized { null != serverChannel }

  override def start = try {
    if (isEnabled) {
      serverChannel = ServerChannel.open(channelGroup)
      serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.box(true))
      serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(aio.defaultBufferSize))
      serverChannel.bind(address, settings.backlog)
      val iteratee = new RequestIteratee()(this)
      reset {
        handle(accept(serverChannel) ++ iteratee.readRequest)
      }
      debug(name + " has started.")
    }
    Server.this
  } catch {
    case e: Throwable ⇒ error(name + " failed to start : " + e); throw e
  }

  override def stop = try {
    if (isStarted) synchronized {
      serverChannel.close
      serverChannel = null
      /**
       * do not shutdown the shared channelGroup here
       */
      debug(name + " has stopped.")
    }
    Server.this
  } catch {
    case e: Throwable ⇒ error(name + " failed to stop : " + e); Server.this
  }

  override def awaitTermination(timeout: Duration) = if (!channelGroup.isShutdown) channelGroup.awaitTermination(if (Duration.Inf == timeout) -1 else timeout.toMillis, TimeUnit.MILLISECONDS)

  final lazy val settings = ServerConfiguration(path, false)

  private[this] var serverChannel: ServerChannel = null

  private[this] final lazy val address = if ("*" == settings.address) new InetSocketAddress(settings.port) else new InetSocketAddress(settings.address, settings.port)

  override final val name = "HttpServer(name=" + settings.displayName + ", address=" + address + ", backlog=" + settings.backlog + ")"

}

/**
 * Contains common things shared among several HttpServers, if any.
 */
object Server {

  /**
   * Do not call channelGroup.shutdown as it prematurely shuts down the threadpool that is shares with akka.
   */
  private final val channelGroup = Group.withThreadPool(concurrent.executor)

  /**
   * A per-server provided configuration, unspecified details will be inherited from defaultServerConfiguration.
   */
  case class ServerConfiguration(path: String, default: Boolean)

    extends CheckedConfig {

    import ServerConfiguration._

    final val settings: Config = config.settings.getConfig(path).withFallback(if (default) fallback else defaultServerConfiguration.settings)

    import settings._

    final val displayName = getString("display-name")

    final val address = getString("address")

    final val port = getInt("port")

    final val backlog = getInt("backlog")

    final val treat10VersionAs11 = getBoolean("feature.allow-version-1.0-but-treat-it-like-1.1")

    final val treatAnyVersionAs11 = getBoolean("feature.allow-any-version-but-treat-it-like-1.1")

    final val defaultCharacterSet = Charset.forName(getString("feature.default-character-set"))

    final val disableUrlDecoding = getBoolean("feature.disable-url-decoding")

  }

  object ServerConfiguration {

    final val fallback = ConfigFactory.parseString("""
        
    display-name = default
	
    address = localhost
	
    port = 7500

    backlog = 1000

    feature {
	
		allow-version-1.0-but-treat-it-like-1.1 = on
	
		allow-any-version-but-treat-it-like-1.1 = off
		
		default-character-set = UTF-8
		
		disable-url-decoding = off
		
	}""")

  }

}

