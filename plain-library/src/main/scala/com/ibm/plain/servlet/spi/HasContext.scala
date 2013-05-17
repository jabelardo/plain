package com.ibm

package plain

package servlet

package spi

import javax.servlet.{ ServletContext ⇒ JServletContext }
import rest.Context

/**
 *
 */
trait HasContext {

  protected[this] val context: Context

  final def getContextPath: String = ""

  final def getContext(uripath: String): JServletContext = unsupported

  final def getServerInfo: String = unsupported

}

