package com.ibm

package plain

package servlet

import javax.servlet.http.{ HttpServletResponse ⇒ JHttpServletResponse }

import rest.Context

/**
 *
 */
final class HttpServletResponse(

  protected[this] val context: Context)

  extends JHttpServletResponse

  with Contexts

  with Contents

  with Buffers

  with Statuses

  with Headers

  with Encodings

  with Sendings

  with Locales

  with Cookies {

}

/**
 *
 */
object HttpServletResponse {

  final def apply(context: Context): HttpServletResponse = new HttpServletResponse(context)

}
