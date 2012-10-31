package com.ibm

package plain

package http

import Status.ClientError.`405`

/**
 * All available http methods inherit from this class.
 */
sealed abstract class Method

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

  case object GET extends Method
  case object HEAD extends Method
  case object POST extends Method
  case object PUT extends Method
  case object DELETE extends Method
  case object OPTIONS extends Method
  case object CONNECT extends Method
  case object TRACE extends Method

}
