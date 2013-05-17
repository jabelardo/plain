package com.ibm

package plain

package servlet

import javax.servlet.http.{ HttpServletRequest ⇒ JHttpServletRequest }

import rest.Context

/**
 *
 */
final class HttpServletRequest private (

  protected[this] final val context: Context)

  extends JHttpServletRequest

  with spi.HttpServletRequest

/**
 *
 */
object HttpServletRequest {

  final def apply(context: Context): HttpServletRequest = new HttpServletRequest(context)

}
