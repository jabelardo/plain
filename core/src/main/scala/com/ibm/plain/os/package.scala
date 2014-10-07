package com.ibm

package plain

import java.net.InetAddress
import org.apache.commons.lang3.SystemUtils._

import config.CheckedConfig
import logging.createLogger
import concurrent.scheduleOnce
import time.timeMillis

package object os

    extends CheckedConfig {

  /**
   * Get the current running operating system. Use match/case on val operatingSystem.
   */
  final val operatingSystem: OperatingSystem = {

    if (IS_OS_AIX)
      OperatingSystem.AIX
    else if (IS_OS_HP_UX)
      OperatingSystem.HPUX
    else if (IS_OS_SOLARIS)
      OperatingSystem.SOLARIS
    else if (IS_OS_LINUX)
      OperatingSystem.LINUX
    else if (IS_OS_MAC_OSX)
      OperatingSystem.OSX
    else if (IS_OS_WINDOWS)
      OperatingSystem.WINDOWS
    else
      OperatingSystem.UNKNOWN
  }

  final val isUnix = IS_OS_UNIX

  final val isLinux = IS_OS_LINUX

  final val isWindows = IS_OS_WINDOWS

  final val isOSX = IS_OS_MAC_OSX

  /**
   * The current user running the JVM.
   */
  final val userName = System.getProperty("user.name")

  /**
   * The machine name the JVM is running on.
   */
  final def hostName = hostname

  /**
   * The canonical hostname the JVM is running on. Depending on DNS settings this might take some time to compute.
   */
  final def canonicalHostName = canonicalhostname

  @volatile private[this] final var hostname = "localhost"

  @volatile private[this] final var canonicalhostname = "localhost"

  @volatile private[plain] final var hostResolved = false

  scheduleOnce(1000) {
    val logger = createLogger(this)
    val (_, millis) = timeMillis {
      hostname = try InetAddress.getLocalHost.getHostName catch {
        case e: Throwable ⇒ logger.error(e.toString); "localhost"
      }
      canonicalhostname = try InetAddress.getLocalHost.getCanonicalHostName catch {
        case e: Throwable ⇒ logger.error(e.toString); "localhost"
      }
      hostResolved = true
    }
    (if (millis > 5000) logger.warn _ else logger.trace _)("Time to resolve hosts: " + millis + "ms, hostName=" + hostName + ", canonicalHostName=" + canonicalHostName)
  }

}
