package com.ibm

package plain

package http

import java.nio.ByteBuffer

import scala.util.control.ControlThrowable

import aio.Io
import aio.Renderable
import aio.Renderable._
import text.`US-ASCII`

/**
 * A typical 'flow control' type of exception. Used for Http error/success handling.
 */
sealed abstract class Status

  extends Throwable

  with Renderable {

  /**
   * Only [0-9]3 are allowed here.
   */
  def code: Array[Byte]

  /**
   * Only US-ASCII characters are allowed here.
   */
  def reason: Array[Byte]

  /**
   * Combination of code and reason for rendering.
   */
  def text: Array[Byte]

  @inline final def render(implicit buffer: ByteBuffer) = r(text) + ^

}

/**
 * The Status object.
 */
object Status {

  sealed abstract class BaseStatus(r: String) extends Status {

    final val code = reflect.simpleName(getClass.getName).getBytes(`US-ASCII`)

    final val reason = r.getBytes(`US-ASCII`)

    final val text = code ++ Array[Byte](' '.toByte) ++ reason

    override final val toString = reflect.simpleParentName(getClass.getName) + "(code=" + new String(code) + ", reason=" + r + ")"

  }

  sealed abstract class Information(r: String) extends BaseStatus(r) with ControlThrowable

  sealed abstract class Success(r: String) extends BaseStatus(r) with ControlThrowable

  sealed abstract class ClientError(r: String) extends BaseStatus(r) with ControlThrowable

  sealed abstract class Redirection(r: String) extends BaseStatus(r) with ControlThrowable

  sealed abstract class ServerError(r: String) extends BaseStatus(r) // here we would like to see the stack trace

  object Information {

    case object `100` extends Information("Continue")

    case object `101` extends Information("Switching Protocals")

  }

  object Success {

    case object `200` extends Success("OK")

    case object `201` extends Success("Created")

    case object `202` extends Success("Accepted")

    case object `203` extends Success("Non-Authoritative Information")

    case object `204` extends Success("No Content")

    case object `205` extends Success("Reset Content")

    case object `206` extends Success("Partial Content")

  }

  object Redirection {

    case object `300` extends Redirection("Multiple Choices")

    case object `301` extends Redirection("Moved Permanently")

    case object `302` extends Redirection("Found")

    case object `303` extends Redirection("See Other")

    case object `304` extends Redirection("Not Modified")

    case object `305` extends Redirection("Use Proxy")

    case object `306` extends Redirection("Unused")

    case object `307` extends Redirection("Temporary Redirect")

  }

  object ClientError {

    case object `400` extends ClientError("Bad Request")

    case object `401` extends ClientError("Unauthorized")

    case object `402` extends ClientError("Payment Required")

    case object `403` extends ClientError("Forbidden")

    case object `404` extends ClientError("Not Found")

    case object `405` extends ClientError("Method Not Allowed")

    case object `406` extends ClientError("Not Acceptable")

    case object `407` extends ClientError("Proxy Authentication Required")

    case object `408` extends ClientError("Request Time-out")

    case object `409` extends ClientError("Conflict")

    case object `410` extends ClientError("Gone")

    case object `411` extends ClientError("Length Required")

    case object `412` extends ClientError("Precondition Failed")

    case object `413` extends ClientError("Request Entity Too Large")

    case object `414` extends ClientError("Request-URI Too Large")

    case object `415` extends ClientError("Unsupported Media Type")

    case object `416` extends ClientError("Requested range not satisfiable")

    case object `417` extends ClientError("Expectation Failed")

  }

  object ServerError {

    case object `500` extends ServerError("Internal Server Error")

    case object `501` extends ServerError("Not Implemented")

    case object `502` extends ServerError("Bad Gateway")

    case object `503` extends ServerError("Service Unavailable")

    case object `504` extends ServerError("Gateway Time-out")

    case object `505` extends ServerError("HTTP Version not supported")

  }

}

