package com.ibm.plain

package lib

package monitor

package extension

import java.lang.management.ManagementFactory

import javax.management.ObjectName

import config.CheckedConfig

package object jmx

  extends CheckedConfig {

  def register = _register

  private[this] final val _register: Unit = {

    ManagementFactory.getPlatformMBeanServer.registerMBean(
      new JmxMonitor,
      new ObjectName("com.ibm.plain.jmx:type=JmxMonitor"))

  }

}
