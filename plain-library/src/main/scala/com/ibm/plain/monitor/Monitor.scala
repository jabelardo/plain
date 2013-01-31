package com.ibm

package plain

package monitor

import scala.collection.JavaConversions.asScalaSet
import scala.concurrent.duration._

import bootstrap.{ BaseComponent, application }
import logging.{ HasLogger, Logging }
import concurrent.scheduleOnce
import aio.Aio._

/**
 * Implementation of a simple Monitor to manage a 'plain' application.
 */
abstract class Monitor

  extends BaseComponent[Monitor]("plain-monitor")

  with HasLogger {

  override def isStarted = isRegistered

  override def start = {
    if (isEnabled) register
    this
  }

  override def stop = {
    if (isStarted) unregister
    this
  }

  protected def register

  protected def unregister

  protected def isRegistered: Boolean

  def getConfiguration: Array[String] = _config

  def getMemoryMbFree = (Runtime.getRuntime.freeMemory / (1024 * 1024)).toInt

  def getMemoryMbMax = (Runtime.getRuntime.maxMemory / (1024 * 1024)).toInt

  def getMemoryMbTotal = (Runtime.getRuntime.totalMemory / (1024 * 1024)).toInt

  def getBufferPoolSizeDefault = defaultBufferPool.size

  def getBufferPoolSizeTiny = tinyBufferPool.size

  def getBufferPoolSizeLarge = largeBufferPool.size

  def getBufferPoolSizeHuge = hugeBufferPool.size

  def getLogLevel = Logging.getLogLevel

  def setLogLevel(level: String) = Logging.setLogLevel(level)

  def getUptimeInSeconds = (application.uptime / 1000).toLong

  def getComponents: Array[String] = application.render

  def shutdown(token: String): String = if (shutdownToken == token) {
    warning("Application teardown was called from JMX console.")
    scheduleOnce(200)(application.teardown)
    "Application teardown started."
  } else {
    error("Application teardown was tried from JMX console, but with invalid token.")
    throw new IllegalArgumentException("Invalid token.")
  }

  private[this] final lazy val _config =
    config.settings.entrySet.map(e ⇒ e.getKey + "=" + e.getValue.render).toArray.sorted

}

