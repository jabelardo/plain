package com.ibm

package plain

package servlet

import javax.servlet.http.{ HttpSession ⇒ JHttpSession }
import rest.Context

/**
 *
 */
final class HttpSession private (

  protected[this] final val context: Context)

  extends JHttpSession

  with spi.HttpSession

/**
 *
 */
object HttpSession {

  final def apply(context: Context) = new HttpSession(context)

}
