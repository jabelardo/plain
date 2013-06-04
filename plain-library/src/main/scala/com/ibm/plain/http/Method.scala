package com.ibm

package plain

package http

import Status.ClientError.`405`

/**
 * All available http methods inherit from this class.
 */
sealed abstract class Method(

  final val safe: Boolean,

  final val idempotent: Boolean,

  final val entityallowed: Boolean)

/**
 * Supported http methods.
 */
object Method {

  final def apply(method: String): Method = method match {
    case _ if 'G' == method(0) ⇒ GET
    case "POST" ⇒ POST
    case "PUT" ⇒ PUT
    case _ if 'H' == method(0) ⇒ HEAD
    case _ if 'D' == method(0) ⇒ DELETE
    case _ if 'O' == method(0) ⇒ OPTIONS
    case _ if 'C' == method(0) ⇒ CONNECT
    case _ if 'T' == method(0) ⇒ TRACE
    case _ ⇒ throw `405`
  }

  /**
   * For GET we convert a query into an entity if this is feasible. Same for HEAD.
   */
  case object GET extends Method(true, true, true)
  case object HEAD extends Method(true, true, true)
  case object POST extends Method(false, false, true)
  case object PUT extends Method(false, true, true)
  case object DELETE extends Method(false, true, false)
  case object OPTIONS extends Method(true, true, true)
  case object CONNECT extends Method(true, true, true)
  case object TRACE extends Method(true, true, false)

}
