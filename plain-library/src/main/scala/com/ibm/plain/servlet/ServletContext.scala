package com.ibm

package plain

package servlet

import javax.servlet.{ ServletContext ⇒ JServletContext }

/**
 *
 */
final class ServletContext private

  extends JServletContext

  with Attributes

  with InitParameters

  with Resources

  with Servlets

  with Dispatchers

  with MimeTypes

  with Logging {

  final val getMajorVersion = 2

  final val getMinorVersion = 5

  final def getContext(uripath: String): JServletContext = unsupported

  final def getContextPath: String = unsupported

  final def getServerInfo: String = unsupported

}

/**
 *
 */
object ServletContext {

  final def apply: ServletContext = new ServletContext

}