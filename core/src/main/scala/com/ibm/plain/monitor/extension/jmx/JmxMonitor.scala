package com.ibm

package plain

package monitor

package extension

package jmx

import java.lang.management.ManagementFactory

import javax.management.ObjectName

import bootstrap.{ IsSingleton, Singleton }

/**
 * A simple JMX monitor to control a running 'plain' application.
 */
trait JmxMonitorMBean {

  def getConfiguration: Array[String]

  def getComponents: Array[String]

  def getMemoryMbFree: Int

  def getMemoryMbMax: Int

  def getMemoryMbTotal: Int

  def getLogLevel: String

  def setLogLevel(level: String)

  def getUptimeInSeconds: Long

  def getBufferPoolSizeDefault: Int

  def getBufferPoolSizeTiny: Int

  def getBufferPoolSizeLarge: Int

  def getBufferPoolSizeHuge: Int

  def shutdown(token: String): String

}

/**
 * Implementation of MonitorMBean
 */
final class JmxMonitor private

    extends Monitor

    with JmxMonitorMBean

    with IsSingleton {

  protected def register = server.registerMBean(
    this,
    name)

  protected def unregister = server.unregisterMBean(name)

  protected def isRegistered = server.isRegistered(name)

  private[this] final val server = ManagementFactory.getPlatformMBeanServer

  private[this] final val name = new ObjectName("com.ibm.plain:type=PlainApplication")

}

/**
 * The Monitor object.
 */
object JmxMonitor

  extends Singleton[JmxMonitor](new JmxMonitor)

