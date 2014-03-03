package com.ibm

package plain

package http

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ AsynchronousChannelGroup ⇒ Group, AsynchronousServerSocketChannel ⇒ ServerChannel }
import java.util.concurrent.Executors.defaultThreadFactory
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.collection.mutable.HashMap

import com.ibm.plain.bootstrap.BaseComponent
import com.typesafe.config.{ Config, ConfigFactory }

import aio.Io.loop
import bootstrap.Application
import config.{ CheckedConfig, config2RichConfig }
import logging.createLogger

/**
 *
 */
final case class Server(

  private val configpath: String,

  private val application: Option[Application],

  private val port: Option[Int],

  private val serverconfig: Option[Server.ServerConfiguration])

  extends BaseComponent[Server] {

  import Server._

  override def isStarted = synchronized { null != serverChannel }

  override def start = try {

    import settings._

    if (isEnabled) {

      def startOne = {
        serverChannel = if (0 == channelGroupThreadPoolType) ServerChannel.open else ServerChannel.open(channelGroup)
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.box(true))
        serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(aio.sendReceiveBufferSize))
        serverChannel.bind(bindaddress, backlog)
        loop(serverChannel, RequestIteratee(this).readRequest, dispatcher)
        logger.debug(name + " has started.")
      }

      application match {
        case Some(appl) if loadBalancingEnable ⇒
          startOne
          portRange.tail.foreach { p ⇒ appl.register(Server(configpath, None, Some(p), Some(settings)).start) }
        case _ ⇒
          startOne
      }
      System.gc
    }
    if (1 < settings.portRange.size && !settings.loadBalancingEnable) logger.warn(name + " : port-range > 1 with load-balancing.enable=off")
    if (settings.portRange.size >= Runtime.getRuntime.availableProcessors) logger.warn("Your port-range size should be smaller than the number of cores available on this system.")
    if (1 == settings.portRange.size && settings.loadBalancingEnable) logger.warn("You cannot enable load-balancing for a port-range of size 1.")
    this
  } catch {
    case e: Throwable ⇒ logger.error(name + " failed to start : " + e); throw e
  }

  override def stop = try {
    if (isStarted) synchronized {
      serverChannel.close
      serverChannel = null
      /**
       * do not shutdown the shared channelGroup here
       */
      logger.debug(name + " has stopped.")
    }
    this
  } catch {
    case e: Throwable ⇒ logger.error(name + " failed to stop : " + e); this
  }

  override def awaitTermination(timeout: Duration) = if (!channelGroup.isShutdown) channelGroup.awaitTermination(if (Duration.Inf == timeout) -1 else timeout.toMillis, TimeUnit.MILLISECONDS)

  override final def name = "HttpServer(name=" + settings.displayName +
    ", address=" + bindaddress +
    ", backlog=" + settings.backlog +
    ", dispatcher=" + { try dispatcher.name catch { case _: Throwable ⇒ "invalid" } } +
    (if (settings.loadBalancingEnable && application.isDefined) ", load-balancing-path=" + settings.loadBalancingBalancingPath else "") +
    ")"

  final def getSettings = settings

  private[this] var serverChannel: ServerChannel = null

  private[this] final lazy val settings = serverconfig match {
    case None ⇒ new ServerConfiguration(configpath, false)
    case Some(s) ⇒ s
  }

  private[this] final lazy val dispatcher = settings.createDispatcher

  private[this] final lazy val bindaddress = if ("*" == settings.address)
    new InetSocketAddress(port.getOrElse(settings.portRange.head))
  else
    new InetSocketAddress(settings.address, port.getOrElse(settings.portRange.head))

  private[this] lazy val logger = createLogger(this)

}

/**
 * Contains common things shared among several HttpServers, the configuration class, for instance.
 */
object Server {

  private final val channelGroup = channelGroupThreadPoolType match {
    // case 0 handled in line 50
    case 1 ⇒ Group.withFixedThreadPool(concurrent.cores, defaultThreadFactory)
    case 2 ⇒ Group.withFixedThreadPool(concurrent.parallelism, defaultThreadFactory)
    case _ ⇒ Group.withThreadPool(concurrent.ioexecutor)
  }

  /**
   * A per-server provided configuration, unspecified details will be inherited from defaultServerConfiguration.
   */
  final class ServerConfiguration(

    val path: String,

    val default: Boolean)

    extends CheckedConfig {

    final val cfg: Config = config.settings.getConfig(path).withFallback(if (default) fallback else defaultServerConfiguration.cfg)

    import cfg._

    final def root = cfg.root

    final val displayName = getString("display-name")

    final val address = getString("address")

    final val portRange = getIntList("port-range", List.empty)

    final val backlog = getBytes("backlog").toInt

    final val loadBalancingEnable = getBoolean("load-balancing.enable")

    final val loadBalancingBalancingPath = getString("load-balancing.balancing-path")

    final val loadBalancingRedirectionPath = getString("load-balancing.redirection-path")

    final val pauseBetweenAccepts = cfg.getDuration("feature.pause-between-accepts", Duration.Zero)

    final val treat10VersionAs11 = getBoolean("feature.allow-version-1-0-but-treat-it-like-1-1")

    final val treatAnyVersionAs11 = getBoolean("feature.allow-any-version-but-treat-it-like-1-1")

    final val defaultCharacterSet = Charset.forName(getString("feature.default-character-set"))

    final val disableUrlDecoding = getBoolean("feature.disable-url-decoding")

    final val maxEntityBufferSize = getBytes("feature.max-entity-buffer-size", 16 * 1024).toInt

    require(0 < portRange.size, "You must at least specify one port for 'port-range'.")

    def createDispatcher = {
      val dconfig = config.settings.getConfig(getString("dispatcher")).withFallback(config.settings.getConfig("plain.rest.default-dispatcher"))

      val dispatcher = dconfig.getInstanceFromClassName[Dispatcher]("class-name")
      dispatcher.name_ = dconfig.getString("display-name", getString("dispatcher"))
      dispatcher.config_ = dconfig
      dispatcher.init
      dispatcher
    }

  }

  final lazy val fallback = ConfigFactory.parseString("""
        
    display-name = default
        
    dispatcher = plain.rest.default-dispatcher
	
    address = "*"
	
    port-range = [ 7500, 7501, 7502 ]

    backlog = 10000

    load-balancing {
    
		enable = on
     
		balancing-path = /
    
		redirection-path = /
    
	}
    
    feature {
        
        pause-between-accepts = 0
	
		allow-version-1-0-but-treat-it-like-1-1 = on
	
		allow-any-version-but-treat-it-like-1-1 = off
		
		default-character-set = ISO-8859-15
		
		disable-url-decoding = off
		
		max-entity-buffer-size = 16K

    }""")

}

