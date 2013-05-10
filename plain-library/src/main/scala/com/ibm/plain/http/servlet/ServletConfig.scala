package com.ibm

package plain

package http

package servlet

import ServletHelpers.InitParameters
import javax.servlet.{ ServletConfig ⇒ JServletConfig, ServletContext ⇒ JServletContext }

/**
 *
 */
trait ServletConfig

  extends JServletConfig

  with InitParameters {

  abstract override def getServletContext: JServletContext = unsupported

  abstract override def getServletName: String = unsupported

}
