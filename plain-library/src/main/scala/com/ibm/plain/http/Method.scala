package com.ibm

package plain

package http

import Status.ClientError.`405`

/**
 * All available http methods inherit from this class.
 */
sealed abstract class Method(

    val safe: Boolean,
    
    val idempotent: Boolean,
    
    val entityallowed: Boolean
    
)

/**
 * Supported http methods.
 */
object Method {

  final def apply(name: String): Method = name match {
    case "GET" ⇒ GET
    case "HEAD" ⇒ HEAD
    case "POST" ⇒ POST
    case "PUT" ⇒ PUT
    case "DELETE" ⇒ DELETE
    case "OPTIONS" ⇒ OPTIONS
    case "CONNECT" ⇒ CONNECT
    case "TRACE" ⇒ TRACE
    case _ ⇒ throw `405`
  }

  case object GET extends Method(true, true, false)
  case object HEAD extends Method(true, true, false)
  case object POST extends Method(false, false, true)
  case object PUT extends Method(false, true, true)
  case object DELETE extends Method(false, true, false)
  case object OPTIONS extends Method(true, true, true)
  case object CONNECT extends Method(true, true, true)
  case object TRACE extends Method(true, true, false)

}
