package com.ibm.plain

package lib

package rest

import java.nio.charset.Charset

import text.UTF8
import aio.Io
import http.Status._
import http.Entity._
import http.Request._
import http.{ Entity, Request, Response, Status }

/**
 *
 */
trait Resource {

  def handle(request: Request): Option[Response]

  def completed(response: Response)

  def failed(e: Throwable)

  def variables: Variables

  def remainder: Path

}

/**
 *
 */
abstract class BaseResource {

  def handle(request: Request): Option[Response] = None

  final def completed(response: Response) = dispatcher.completed(response, io)

  final def failed(e: Throwable) = dispatcher.failed(e, io)

  def variables = variables_

  def remainder = remainder_

  private[rest] var variables_ : Variables = null

  private[rest] var remainder_ : Path = null

  private[rest] var dispatcher: RestDispatcher = null

  protected[rest] var io: Io = null

}
