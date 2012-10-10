package com.ibm.plain

package lib

package monitor

import scala.collection.JavaConversions.asScalaSet
import scala.concurrent.util.Duration

import com.ibm.plain.lib.bootstrap.BaseComponent

import bootstrap.{ BaseComponent, application }
import logging.{ HasLogger, Logging }
import concurrent.scheduleOnce

/**
 * Implementation of a simple Monitor to manage a 'plain' application.
 */
abstract class Monitor

  extends BaseComponent[Monitor]("plain-monitor")

  with HasLogger {

  def isStarted = isRegistered

  def isStopped = !isStarted

  def start = {
    if (isEnabled) register
    this
  }

  def stop = {
    if (isStarted) unregister
    this
  }

  def awaitTermination(timeout: Duration) = ()

  protected def register

  protected def unregister

  protected def isRegistered: Boolean

  def getConfiguration: Array[String] = _config

  def getFreeMemoryMb = (Runtime.getRuntime.freeMemory / (1024 * 1024)).toInt

  def getMaxMemoryMb = (Runtime.getRuntime.maxMemory / (1024 * 1024)).toInt

  def getTotalMemoryMb = (Runtime.getRuntime.totalMemory / (1024 * 1024)).toInt

  def getLogLevel = Logging.getLogLevel

  def setLogLevel(level: String) = Logging.setLogLevel(level)

  def getUptimeInSeconds = (application.uptime / 1000).toLong

  def getComponents = application.toString

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

