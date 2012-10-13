package com.ibm.plain

package lib

import config.CheckedConfig
import org.apache.commons.lang3.SystemUtils._
import java.net.InetAddress

package object os

  extends CheckedConfig {

  /**
   * Get the current running operating system. Use match/case on val operatingSystem.
   */
  final val operatingSystem: OperatingSystem =
    if (IS_OS_AIX)
      AIX
    else if (IS_OS_HP_UX)
      HPUX
    else if (IS_OS_SOLARIS)
      SOLARIS
    else if (IS_OS_LINUX)
      LINUX
    else if (IS_OS_MAC_OSX)
      OSX
    else if (IS_OS_WINDOWS)
      WINDOWS
    else
      UNKNOWN

  final val isUnix = IS_OS_UNIX

  final val isWindows = IS_OS_WINDOWS

  /**
   * The current user running the JVM.
   */
  final val userName = System.getProperty("user.name")

  /**
   * The machine name the JVM is running on.
   */
  final val hostName = InetAddress.getLocalHost.getHostName

  /**
   * The canonical hostname the JVM is running on. Depending on DNS settings this might take some time to compute.
   */
  final lazy val canonicalHostName = InetAddress.getLocalHost.getCanonicalHostName

  sealed abstract class OperatingSystem(value: String) {

    override lazy val toString = "OperatingSystem(" + value + ")"

  }

  case object AIX extends OperatingSystem("AIX")

  case object HPUX extends OperatingSystem("HPUX")

  case object SOLARIS extends OperatingSystem("SOLARIS")

  case object LINUX extends OperatingSystem("LINUX")

  case object OSX extends OperatingSystem("OSX")

  case object WINDOWS extends OperatingSystem("WINDOWS")

  case object UNKNOWN extends OperatingSystem("UNKNOWN")

}
