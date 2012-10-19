package com.ibm.plain

package lib

package http

import java.nio.CharBuffer

import scala.util.control.ControlThrowable

import ResponseConstants._

/**
 * A typical 'flow control' type of exception. Used for Http error/success handling.
 */
sealed abstract class Status

  extends ControlThrowable

  with Renderable {

  val code: String

  val reason: String

  @inline final def render(implicit buffer: CharBuffer) = buffer.put(code).put(` `).put(reason)

}

/**
 * The HttpStatus object.
 */
object Status {

  abstract sealed class BaseStatus extends Status {

    final lazy val code = reflect.simpleName(getClass)

    override lazy val toString = reflect.simpleParentName(getClass) + "(code=" + code + ", reason=" + reason + ")"

  }

  sealed trait Information extends BaseStatus

  sealed trait Success extends BaseStatus

  sealed trait ClientError extends BaseStatus

  sealed trait Redirection extends BaseStatus

  sealed trait ServerError extends BaseStatus

  object Information {

    case class `100`(reason: String) extends Information { def this() = this("Continue") }

    case class `101`(reason: String) extends Information { def this() = this("Switching Protocols") }

  }

  object Success {

    case class `200`(reason: String) extends Success { def this() = this("OK") }

    case class `201`(reason: String) extends Success { def this() = this("Created") }

    case class `202`(reason: String) extends Success { def this() = this("Accepted") }

    case class `203`(reason: String) extends Success { def this() = this("Non-Authoritative Information") }

    case class `204`(reason: String) extends Success { def this() = this("No Content") }

    case class `205`(reason: String) extends Success { def this() = this("Reset Content") }

    case class `206`(reason: String) extends Success { def this() = this("Partial Content") }

  }

  object Redirection {

    case class `300`(reason: String) extends Redirection { def this() = this("Multiple Choices") }

    case class `301`(reason: String) extends Redirection { def this() = this("Moved Permanently") }

    case class `302`(reason: String) extends Redirection { def this() = this("Found") }

    case class `303`(reason: String) extends Redirection { def this() = this("See Other") }

    case class `304`(reason: String) extends Redirection { def this() = this("Not Modified") }

    case class `305`(reason: String) extends Redirection { def this() = this("Use Proxy") }

    case class `306`(reason: String) extends Redirection { def this() = this("Unused") }

    case class `307`(reason: String) extends Redirection { def this() = this("Temporary Redirect") }

  }

  object ClientError {

    case class `400`(reason: String) extends ClientError { def this() = this("Bad Request") }

    case class `401`(reason: String) extends ClientError { def this() = this("Unauthorized") }

    case class `402`(reason: String) extends ClientError { def this() = this("Payment Required") }

    case class `403`(reason: String) extends ClientError { def this() = this("Forbidden") }

    case class `404`(reason: String) extends ClientError { def this() = this("Not Found") }

    case class `405`(reason: String) extends ClientError { def this() = this("Method Not Allowed") }

    case class `406`(reason: String) extends ClientError { def this() = this("Not Acceptable") }

    case class `407`(reason: String) extends ClientError { def this() = this("Proxy Authentication Required") }

    case class `408`(reason: String) extends ClientError { def this() = this("Request Time-out") }

    case class `409`(reason: String) extends ClientError { def this() = this("Conflict") }

    case class `410`(reason: String) extends ClientError { def this() = this("Gone") }

    case class `411`(reason: String) extends ClientError { def this() = this("Length Required") }

    case class `412`(reason: String) extends ClientError { def this() = this("Precondition Failed") }

    case class `413`(reason: String) extends ClientError { def this() = this("Request Entity Too Large") }

    case class `414`(reason: String) extends ClientError { def this() = this("Request-URI Too Large") }

    case class `415`(reason: String) extends ClientError { def this() = this("Unsupported Media Type") }

    case class `416`(reason: String) extends ClientError { def this() = this("Requested range not satisfiable") }

    case class `417`(reason: String) extends ClientError { def this() = this("Expectation Failed") }

  }

  object ServerError {

    case class `500`(reason: String) extends ServerError { def this() = this("Internal Server Error") }

    case class `501`(reason: String) extends ServerError { def this() = this("Not Implemented") }

    case class `502`(reason: String) extends ServerError { def this() = this("Bad Gateway") }

    case class `503`(reason: String) extends ServerError { def this() = this("Service Unavailable") }

    case class `504`(reason: String) extends ServerError { def this() = this("Gateway Time-out") }

    case class `505`(reason: String) extends ServerError { def this() = this("HTTP Version not supported") }

  }

}

