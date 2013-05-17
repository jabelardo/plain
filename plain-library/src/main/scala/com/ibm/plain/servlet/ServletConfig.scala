package com.ibm

package plain

package servlet

import javax.servlet.{ ServletConfig ⇒ JServletConfig }
import rest.Context

/**
 *
 */
final class ServletConfig private (

  protected[this] final val context: Context)

  extends JServletConfig

  with spi.ServletConfig

/**
 *
 */
object ServletConfig {

  final def apply(context: Context): ServletConfig = new ServletConfig(context)

}

