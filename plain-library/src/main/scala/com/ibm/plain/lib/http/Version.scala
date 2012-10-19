package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import scala.util.control.NoStackTrace

import aio.Io

import Status.ServerError.`505`

/**
 * Supported http versions. The current implementation only supports HTTP/1.1.
 */
sealed abstract class Version {

  final val version = reflect.simpleName(getClass)

}

object Version {

  def apply(version: String)(implicit server: Server): Version = version match {
    case "HTTP/1.0" if server.settings.treat10VersionAs11 ⇒ `HTTP/1.1`
    case "HTTP/1.1" ⇒ `HTTP/1.1`
    case v ⇒
      if (server.settings.treatAnyVersionAs11)
        `HTTP/1.1`
      else
        throw new `505`
  }

  /**
   * We implement support for HTTP/1.1 only, but eventually allow 1.0 and treat it like 1.1.
   */
  case object `HTTP/1.0` extends Version
  case object `HTTP/1.1` extends Version

}

