package com.ibm

package plain

package http

package servlet

import javax.servlet.http.{ HttpServletResponse ⇒ JHttpServletResponse }

import ServletHelpers._

/**
 *
 */
final class HttpServletResponse

  extends JHttpServletResponse

  with Contents

  with Buffers

  with Statuses

  with Headers

  with Encodings
  
  with Sendings

  with Locales

  with Cookies {

}
