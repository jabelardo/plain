package com.ibm

package plain

package http

package servlet

import javax.servlet.http.{ HttpServletRequest ⇒ JHttpServletRequest }

import ServletHelpers._

/**
 *
 */
final class HttpServletRequest

  extends JHttpServletRequest

  with Attributes

  with Parameters

  with Dispatchers

  with Contents

  with Locales

  with NetInfos

  with Headers

  with Sessions

  with Users

  with Paths

  with Cookies {

}
