package com.ibm.plain

package lib

package rest

import java.nio.charset.Charset

import text.UTF8

import http.Status._
import http.Entity._
import http.{ Entity, Request, Status }

import Resource._

/**
 * The classic rest resource.
 */
trait Resource {

  def request: Request

  def get(entity: Option[Entity]): (Status, Option[Entity])

  def head(entity: Option[Entity]): Status

  def post(entity: Option[Entity]): (Status, Option[Entity])

  def put(entity: Option[Entity]): (Status, Option[Entity])

  def delete(entity: Option[Entity]): (Status, Option[Entity])

  def options(entity: Option[Entity]): (Status, Option[Entity])

  def connect(entity: Option[Entity]): Status

  def trace(entity: Option[Entity]): (Status, Entity)

  /**
   * convenience methods for the most common entity types.
   */
  def get: (Status, Option[Entity])

  def head: Status

  def post(s: String): (Status, Option[Entity])

  def put(s: String): (Status, Option[Entity])

  def delete: (Status, Option[Entity])

  def delete(s: String): (Status, Option[Entity])

}

/**
 * A basic implementation of Resource.
 */
abstract class BaseResource

  extends Resource {

  final var request: Request = null

  def get(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def head(entity: Option[Entity]): Status = get(entity)._1

  def post(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def put(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def delete(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def options(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def connect(entity: Option[Entity]): Status = ClientError.`405`

  def trace(entity: Option[Entity]): (Status, Entity) = (Success.`200`, BytesEntity(request.toString.getBytes(UTF8)))

  def get: (Status, Option[Entity]) = get(None)

  def head: Status = head(None)

  def post(s: String): (Status, Option[Entity]) = post(Some(BytesEntity(s.getBytes(UTF8))))

  def put(s: String): (Status, Option[Entity]) = put(Some(BytesEntity(s.getBytes(UTF8))))

  def delete: (Status, Option[Entity]) = delete(None)

  def delete(s: String): (Status, Option[Entity]) = delete(Some(BytesEntity(s.getBytes(UTF8))))

}

/**
 * Often used helpers for users of this class.
 */
object Resource {

  final def Ok(s: String): (Status, Option[Entity]) = Ok(s, UTF8)

  final def Ok(s: String, cset: Charset) = (Success.`200`, Some(BytesEntity(s.getBytes(cset))))

}
  