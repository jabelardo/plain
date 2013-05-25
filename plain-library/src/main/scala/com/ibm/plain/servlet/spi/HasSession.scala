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

  final def getSession: JHttpSession = { println("getSession"); session }

  final def getSession(create: Boolean): JHttpSession = { println("getSession " + create); session }

  final def getRequestedSessionId: String = { println("getRequestedSessionId"); "" }

  final def isRequestedSessionIdFromCookie: Boolean = unsupported

  final def isRequestedSessionIdValid: Boolean = unsupported

  final def isRequestedSessionIdFromURL: Boolean = unsupported

  final def isRequestedSessionIdFromUrl: Boolean = isRequestedSessionIdFromURL

  val session = HttpSession(context)

}

