package com.ibm

package plain

package servlet

import javax.servlet.http.{ HttpServletRequest ⇒ JHttpServletRequest }

import rest.Context

/**
 *
 */
final class HttpServletRequest private (

  protected[this] val context: Context)

  extends JHttpServletRequest

  with Contexts

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

/**
 *
 */
object HttpServletRequest {

  final def apply(context: Context): HttpServletRequest = new HttpServletRequest(context)

}
