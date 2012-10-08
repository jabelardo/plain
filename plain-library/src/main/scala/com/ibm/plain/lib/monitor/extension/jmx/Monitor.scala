package com.ibm.plain

package lib

package monitor

package extension

package jmx

/**
 * A simple JMX monitor to control a running 'plain' application.
 */
trait JmxMonitorMBean {

  def getConfiguration: Array[String]

  def getFreeMemoryMb: Int

  def getMaxMemoryMb: Int

  def getTotalMemoryMb: Int

  def getLogLevel: String

  def setLogLevel(level: String)

  def getUptimeInSeconds: Long

  def shutdown(token: String): String

}

/**
 * Implementation of MonitorMBean
 */
class JmxMonitor

  extends Monitor

  with JmxMonitorMBean

