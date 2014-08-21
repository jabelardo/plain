package com.ibm

package plain

package monitor

import com.ibm.plain.bootstrap.BaseComponent

import scala.collection.JavaConversions.asScalaSet

import aio.Aio
import bootstrap.{ Application, BaseComponent }
import concurrent.scheduleOnce
import logging.{ createLogger, loggingLevel, setLoggingLevel }

/**
 * Implementation of a simple Monitor to manage a 'plain' application.
 */
abstract class Monitor

  extends BaseComponent[Monitor]("plain-monitor") {

  override def isStopped = !isRegistered

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

  def getBufferPoolSizeDefault = Aio.instance.defaultBufferPool.size

  def getBufferPoolSizeTiny = Aio.instance.tinyBufferPool.size

  def getBufferPoolSizeLarge = Aio.instance.largeBufferPool.size

  def getBufferPoolSizeHuge = Aio.instance.hugeBufferPool.size

  def getLogLevel = loggingLevel

  def setLogLevel(level: String) = setLoggingLevel(level)

  def getUptimeInSeconds = (Application.instance.uptime / 1000).toLong

  def getComponents: Array[String] = Application.instance.render

  def shutdown(token: String): String = if (shutdownToken == token) {
    createLogger(this).warn("Application teardown was called from JMX console.")
    scheduleOnce(200)(Application.instance.teardown)
    "Application teardown started."
  } else {
    createLogger(this).error("Application teardown was tried from JMX console, but with invalid token.")
    throw new IllegalArgumentException("Invalid token.")
  }

  private[this] final lazy val _config =
    config.settings.entrySet.map(e ⇒ e.getKey + "=" + e.getValue.render).toArray.sorted

}

