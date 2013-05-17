package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasSocket {

  self: HasContext ⇒

  final def getProtocol: String = http.Version.`HTTP/1.1`.toString

  final def getScheme: String = "http"

  final def isSecure: Boolean = false

  final def getRealPath(path: String): String = deprecated

  final def getLocalName: String = "localaddr"

  final def getLocalPort: Int = 0

  final def getLocalAddr: String = "localaddr"

  final def getRemoteName: String = "remotename"

  final def getRemotePort: Int = unsupported

  final def getRemoteAddr: String = "remoteaddr"

  final def getRemoteHost: String = "remotehost"

  final def getServerName: String = "servername"

  final def getServerPort: Int = 0

}

