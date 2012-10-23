package com.ibm.plain

package lib

package http

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ AsynchronousChannelGroup ⇒ Group, AsynchronousServerSocketChannel ⇒ ServerChannel }
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.util.continuations.reset

import com.typesafe.config.{ Config, ConfigFactory }

import aio.Io.{ accept, loop }
import bootstrap.{ Application, BaseComponent }
import logging.HasLogger
import config.{ CheckedConfig, config2RichConfig }

/**
 *
 */
case class Server(

  private val path: String,

  private val application: Option[Application],

  private val port: Option[Int])

  extends BaseComponent[Server](null)

  with HasLogger {

  import Server._

  override def isStarted = synchronized { null != serverChannel }

  override def start = try {
    if (isEnabled) {

      def startOne = {
        serverChannel = ServerChannel.open(channelGroup)
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.box(true))
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(aio.defaultBufferSize))
        serverChannel.bind(address, settings.backlog)
        val iteratee = new RequestIteratee()(this)
        reset {
          loop(
            accept(serverChannel, settings.pauseBetweenAccepts) ++ iteratee.readRequest,
            new HttpProcessor)
        }
        debug(name + " has started.")
      }

      application match {
        case Some(appl) if settings.loadBalancingEnable ⇒
          startOne
          settings.portRange.tail.foreach { p ⇒ appl.register(Server(path, None, Some(p)).start) }
        case _ ⇒ startOne
      }
    }
    this
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
    this
  } catch {
    case e: Throwable ⇒ error(name + " failed to stop : " + e); this
  }

  override def awaitTermination(timeout: Duration) = if (!channelGroup.isShutdown) channelGroup.awaitTermination(if (Duration.Inf == timeout) -1 else timeout.toMillis, TimeUnit.MILLISECONDS)

  private[this] var serverChannel: ServerChannel = null

  private[http] final lazy val settings = ServerConfiguration(path, false)

  private[this] final lazy val address = if ("*" == settings.address)
    new InetSocketAddress(port.getOrElse(settings.portRange.head))
  else
    new InetSocketAddress(settings.address, port.getOrElse(settings.portRange.head))

  override final val name = "HttpServer(name=" + settings.displayName +
    ", address=" + address +
    ", backlog=" + settings.backlog +
    (if (settings.loadBalancingEnable && application.isDefined) ", load-balancing-path=" + settings.loadBalancingBalancingPath else "") +
    ")"

  if (1 < settings.portRange.size && !settings.loadBalancingEnable) warning(name + " : port-range > 1 with load-balancing.enable=off")

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

    final val portRange = getIntList("port-range", List.empty)

    final val backlog = getInt("backlog")

    final val loadBalancingEnable = getBoolean("feature.load-balancing.enable")

    final val loadBalancingBalancingPath = getString("feature.load-balancing.balancing-path")

    final val loadBalancingRedirectionPath = getString("feature.load-balancing.redirection-path")

    final val pauseBetweenAccepts = settings.getDuration("feature.pause-between-accepts")

    final val treat10VersionAs11 = getBoolean("feature.allow-version-1-0-but-treat-it-like-1-1")

    final val treatAnyVersionAs11 = getBoolean("feature.allow-any-version-but-treat-it-like-1-1")

    final val defaultCharacterSet = Charset.forName(getString("feature.default-character-set"))

    final val disableUrlDecoding = getBoolean("feature.disable-url-decoding")

    require(0 < portRange.size, "You must at least specify one port for 'port-range'.")

    require(if (1 == portRange.size) !loadBalancingEnable else true, "You cannot enable load-balancing for a port-range of size 1.")

    require(portRange.size < Runtime.getRuntime.availableProcessors, "Your port-range size must be smaller than the number of cores available on this system.")

  }

  object ServerConfiguration {

    final val fallback = ConfigFactory.parseString("""
        
    display-name = default
	
    address = localhost
	
    port-range = [ 7500, 7501, 7502 ]

    backlog = 1000

    feature {
        
        load-balancing {
        
    		enable = on
         
    		balancing-path = /
        
    		redirection-path = /
        
    	}
        
        pause-between-accepts = 0
	
		allow-version-1-0-but-treat-it-like-1-1 = on
	
		allow-any-version-but-treat-it-like-1-1 = off
		
		default-character-set = UTF-8
		
		disable-url-decoding = off
		
	}""")

  }

}

