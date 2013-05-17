package com.ibm

package plain

package servlet

import javax.servlet.{ ServletContext ⇒ JServletContext }
import rest.Context

/**
 *
 */
final class ServletContext private (

  protected[this] final val context: Context)

  extends JServletContext

  with spi.ServletContext

/**
 *
 */
object ServletContext {

  final def apply(context: Context): ServletContext = new ServletContext(context)

}

