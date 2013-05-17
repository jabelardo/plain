package com.ibm

package plain

package servlet

package spi

import javax.servlet.http.{ HttpSession ⇒ JHttpSession }

/**
 *
 */
trait HasSession {

  self: HasContext ⇒

  final def getSession: JHttpSession = HttpSession(context)

  final def getSession(create: Boolean): JHttpSession = HttpSession(context)

  final def getRequestedSessionId: String = time.now.toString

  final def isRequestedSessionIdFromCookie: Boolean = false

  final def isRequestedSessionIdValid: Boolean = true

  final def isRequestedSessionIdFromURL: Boolean = true

  final def isRequestedSessionIdFromUrl: Boolean = isRequestedSessionIdFromURL

}

