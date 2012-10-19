package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import scala.util.control.NoStackTrace

import aio.Io

import Status.ClientError.`405`

/**
 * Supported http methods.
 */
sealed abstract class Method(val name: String)

object Method {

  def apply(name: String): Method = name match {
    case "GET" ⇒ GET
    case "HEAD" ⇒ HEAD
    case "PUT" ⇒ PUT
    case "POST" ⇒ POST
    case "DELETE" ⇒ DELETE
    case "OPTIONS" ⇒ OPTIONS
    case "CONNECT" ⇒ CONNECT
    case "TRACE" ⇒ TRACE
    case n ⇒ throw `405`("Invalid method " + n)
  }

  case object GET extends Method("GET")
  case object HEAD extends Method("HEAD")
  case object PUT extends Method("PUT")
  case object POST extends Method("POST")
  case object DELETE extends Method("DELETE")
  case object OPTIONS extends Method("OPTIONS")
  case object CONNECT extends Method("CONNECT")
  case object TRACE extends Method("TRACE")

}
