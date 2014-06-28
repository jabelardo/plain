package com.ibm

package plain

package os

sealed abstract class OperatingSystem(value: String)

/**
 * The most important OSs. It does not mean we support them all. Our code was tested on AIX, Linux, OSX and Windows.
 */
object OperatingSystem {

  case object AIX extends OperatingSystem("AIX")

  case object HPUX extends OperatingSystem("HPUX")

  case object SOLARIS extends OperatingSystem("SOLARIS")

  case object LINUX extends OperatingSystem("LINUX")

  case object OSX extends OperatingSystem("OSX")

  case object WINDOWS extends OperatingSystem("WINDOWS")

  case object UNKNOWN extends OperatingSystem("UNKNOWN")

}

