package com.ibm

package plain

package servlet

import javax.servlet.http.{ HttpServletResponse ⇒ JHttpServletResponse }
import rest.Context

/**
 *
 */
final class HttpServletResponse private (

  protected[this] final val context: Context)

  extends JHttpServletResponse

  with spi.HttpServletResponse

/**
 *
 */
object HttpServletResponse {

  final def apply(context: Context): HttpServletResponse = new HttpServletResponse(context)

}
