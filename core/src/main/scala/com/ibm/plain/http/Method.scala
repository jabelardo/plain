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

  final val readonly: Boolean,

  final val entityallowed: Boolean)

/**
 * Supported http methods.
 */
object Method {

  final def apply(method: String): Method = method match {
    case "GET"     ⇒ GET
    case "HEAD"    ⇒ HEAD
    case "POST"    ⇒ POST
    case "PUT"     ⇒ PUT
    case "DELETE"  ⇒ DELETE
    case "OPTIONS" ⇒ OPTIONS
    case "CONNECT" ⇒ CONNECT
    case "TRACE"   ⇒ TRACE
    case _         ⇒ throw `405`
  }

  /**
   * For GET we convert a query into an entity if this is feasible. Same for HEAD.
   */
  case object GET extends Method(true, true, true, true)
  case object HEAD extends Method(true, true, true, true)
  case object POST extends Method(false, false, false, true)
  case object PUT extends Method(false, true, false, true)
  case object DELETE extends Method(false, true, false, false)
  case object OPTIONS extends Method(true, true, true, true)
  case object CONNECT extends Method(true, true, true, true)
  case object TRACE extends Method(true, true, false, false)

}
