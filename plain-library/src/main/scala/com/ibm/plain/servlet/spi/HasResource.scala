package com.ibm

package plain

package servlet

package spi

import java.io.InputStream
import java.net.URL
import java.util.{ Set ⇒ JSet }

/**
 *
 */
trait HasResource {

  final def getResource(path: String): URL = unsupported

  final def getResourceAsStream(path: String): InputStream = unsupported

  final def getResourcePaths(path: String): JSet[String] = unsupported

  final def getRealPath(path: String): String = unsupported

}
