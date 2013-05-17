package com.ibm

package plain

package servlet

package spi

import java.util.Enumeration

import javax.servlet.{ Servlet ⇒ JServlet, ServletContext ⇒ JServletContext }

/**
 *
 */
trait HasServlet {

  self: HasContext ⇒

  final def getServlet(name: String): JServlet = deprecated

  final def getServletNames: Enumeration[String] = deprecated

  final def getServlets: Enumeration[JServlet] = deprecated

  final def getServletName: String = unsupported

  final def getServletContextName: String = unsupported

  final def getServletContext: JServletContext = ServletContext(context)

}

