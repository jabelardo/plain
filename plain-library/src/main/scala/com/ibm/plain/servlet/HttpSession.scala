package com.ibm

package plain

package servlet

import javax.servlet.{ ServletContext ⇒ JServletContext }
import javax.servlet.http.{ HttpSession ⇒ JHttpSession, HttpSessionContext }

/**
 *
 */
final class HttpSession private

  extends JHttpSession

  with Attributes

  with Values

  with Times {

  final def getId: String = unsupported

  final def getServletContext: JServletContext = unsupported

  @deprecated("2.1", "will be removed") final def getSessionContext: HttpSessionContext = deprecated

  final def invalidate = unsupported

}

object HttpSession {

  final def apply = new HttpSession

}
