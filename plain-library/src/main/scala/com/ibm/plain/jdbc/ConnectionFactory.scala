package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection }
import java.util.Properties
import java.util.concurrent.{ ConcurrentHashMap, LinkedBlockingDeque, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet }
import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

import com.ibm.plain.bootstrap.BaseComponent
import com.typesafe.config.{ Config, ConfigList, ConfigValue, ConfigFactory, ConfigValueFactory }

import akka.actor.Cancellable

import bootstrap.BaseComponent
import concurrent.schedule
import config.config2RichConfig
import javax.sql.DataSource
import logging.HasLogger
import reflect.primitive
import time.now

/**
 *
 */
final case class ConnectionFactory(

  configpath: String)

  extends BaseComponent[ConnectionFactory]

  with HasLogger {

  override def name = getClass.getSimpleName + "(name=" + displayname + ", config=" + configpath + ", pool=" + poolmin + "/" + poolmax + ")"

  final def dataSource = datasource

  override final def start = {
    setParameters(datasource, datasourcesettings)
    setProperties(datasourceproperties)
    try {
      val poolmins = new ListBuffer[Connection]
      (0 until poolmin).foreach { i ⇒
        getConnection() match {
          case Some(connection) ⇒ poolmins += connection
          case None ⇒
        }
      }
      poolmins.foreach { _.close }
    } catch {
      case e: Throwable ⇒ error(name + " : Cannot establish connection :  " + e)
    }
    connectioncleaner = schedule(idletimeout) {
      if (idle.size > poolmin) {
        idle.peekLast match {
          case null ⇒
          case peekonly ⇒ if (idletimeout < (now - peekonly.lastaccessed.get)) {
            idle.pollLast match {
              case null ⇒
              case reallypoll if reallypoll == peekonly ⇒
                connections.remove(reallypoll)
                reallypoll.doClose
            }
          }
        }
      }
    }
    debug(name + " has started.")
    this
  }

  override final def stop = {
    if (isStarted) {
      if (null != connectioncleaner) connectioncleaner.cancel
      closeConnections
      debug(name + " has stopped.")
    }
    this
  }

  final def newConnection(timeout: Long = pooltimeout) = getConnection(timeout) match {
    case Some(connection) ⇒ connection
    case _ ⇒ throw new java.sql.SQLTimeoutException("No connection available from " + name)
  }

  final def getConnection(timeout: Long = pooltimeout): Option[Connection] = {
    var connection: Option[Connection] = None
    var elapsed = 0L
    val interval = growtimeout.get
    try {
      while (None == connection && elapsed < timeout) {
        connection = idle.poll(interval, TimeUnit.MILLISECONDS) match {
          case null if connections.size < poolmax ⇒
            val conn = datasource.getConnection.unwrap(connectionclass).asInstanceOf[JdbcConnection]
            setParameters(conn, connectionsettings)
            val connection = new Connection(conn, idle)
            connections.add(connection)
            connection.activate
            Some(connection)
          case null ⇒
            elapsed += interval
            None
          case connection ⇒
            connection.lastaccessed.set(now)
            connection.activate
            Some(connection)
        }
      }
      if (elapsed > peakelapsed.get) {
        peakelapsed.set(elapsed)
        if (log.isDebugEnabled) debug(name + " : peak elapsed : " + elapsed)
      }
      if (None == connection) error(name + ": " + "No more connections available in pool.")

      connection
    } catch {
      case e: Throwable ⇒
        error("Datasource '" + name + "' cannot etablish connection: " + e)
        None
    }
  }

  private[this] final def closeConnections = try {
    growtimeout.set(Long.MaxValue)
    idle.clear
    connections.keySet.foreach { connection ⇒
      connection.doClose
    }
    connections.clear
  } catch {
    case e: Throwable ⇒ error(name + " : " + e)
  }

  private[this] final def setParameters(any: Any, config: Config) = {
    config.entrySet.foreach(entry ⇒ {
      val methodname = entry.getKey.split("""\.""").last
      val parameters = entry.getValue.unwrapped.asInstanceOf[java.util.List[Any]]
      invoke(any, methodname, parameters.toArray)
    })
  }

  private[this] final def setProperties(properties: ConfigList) = if (0 < properties.size) {
    val property = new Properties
    properties.foreach { entry ⇒
      val list = entry.unwrapped.asInstanceOf[java.util.List[Object]]
      require(2 <= list.size, "Invalid property : " + list)
      property.setProperty(list(0).toString, list(1).toString)
    }
    invoke(datasource, datasourcepropertiessetter, Array(property))
  }

  private[this] final def invoke(any: Any, methodname: String, parameters: Array[AnyRef]) = {
    val parametertypes = parameters map (t ⇒ primitive(t.getClass))
    val method = any.getClass.getMethod(methodname, parametertypes: _*)
    method.setAccessible(true)
    method.invoke(any, parameters: _*)
  }

  private[this] val settings = config.settings.getConfig(configpath).withFallback(config.settings.getConfig("plain.jdbc.default-connection-factory"))

  private[this] final val connections = new ConcurrentHashMap[Connection, Unit] {

    final def add(connection: Connection) = put(connection, ())

    override final def put(connection: Connection, o: Unit): Unit = {
      super.put(connection, o)
      if (size > peak.get) peak.set(size)
      // if (log.isDebugEnabled) debug(name + ": " + "Connections.add " + size + ", peak " + peak.get)
    }

    final def remove(connection: Connection): Unit = {
      super.remove(connection)
      // if (log.isDebugEnabled) debug(name + ": " + "Connections.remove: connections " + size + ", peak " + peak.get + ", timeout " + (now - connection.asInstanceOf[Connection].lastaccessed.get) + " ms, idle " + idle.size)
    }

    private[this] final val peak = new AtomicInteger(0)

  }

  private[this] final val peakelapsed = new AtomicLong(0L)

  private[this] final val idle = new LinkedBlockingDeque[Connection]

  private[this] final var connectioncleaner: Cancellable = null

  private[this] final val driver = settings.getString("driver")

  private[jdbc] final val displayname = settings.getString("display-name", "default")

  private[this] final val growtimeout = new AtomicLong(settings.getMilliseconds("pool-grow-timeout", 200))

  private[this] final val idletimeout = settings.getMilliseconds("pool-idle-timeout", 300000)

  private[this] final val pooltimeout = settings.getMilliseconds("pool-get-timeout", 15000)

  private[this] final val poolmin = settings.getInt("min-pool-size", 1)

  private[this] final val poolmax = settings.getInt("max-pool-size", 8)

  require(poolmin <= poolmax, "min-pool-size is larger than max-pool-size (" + poolmin + ", " + poolmax + ")")

  private[this] final val connectionclass = Class.forName(config.settings.getString("plain.jdbc.drivers." + driver + ".connection-class", "java.sql.Connection"))

  private[this] final val datasource = config.settings.getInstanceFromClassName[javax.sql.DataSource]("plain.jdbc.drivers." + driver + ".datasource-class")

  private[this] final val datasourcesettings = settings.getConfig("datasource-settings", ConfigFactory.empty).withFallback(config.settings.getConfig("plain.jdbc.drivers." + driver + ".datasource-settings", ConfigFactory.empty))

  private[this] final val datasourceproperties = settings.withFallback(config.settings.getConfig("plain.jdbc.drivers." + driver)).getList("datasource-properties", ConfigValueFactory.fromIterable(new java.util.LinkedList))

  private[this] final val datasourcepropertiessetter = settings.withFallback(config.settings.getConfig("plain.jdbc.drivers." + driver)).getString("datasource-properties-setter", "")

  private[this] final val connectionsettings = settings.getConfig("connection-settings", ConfigFactory.empty).withFallback(config.settings.getConfig("plain.jdbc.drivers." + driver + ".connection-settings", ConfigFactory.empty))

}
