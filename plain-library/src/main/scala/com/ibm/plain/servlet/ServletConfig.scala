package com.ibm

package plain

package servlet

import javax.servlet.{ ServletConfig ⇒ JServletConfig, ServletContext ⇒ JServletContext }

/**
 *
 */
final class ServletConfig private

  extends JServletConfig

  with InitParameters {

  def getServletContext: JServletContext = ServletContext.apply

  def getServletName: String = unsupported

}

/**
 *
 */
object ServletConfig {

  final def apply: ServletConfig = new ServletConfig

}
