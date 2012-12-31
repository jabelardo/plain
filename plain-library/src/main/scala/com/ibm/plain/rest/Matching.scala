package com.ibm

package plain

package rest

import java.nio.charset.Charset

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.xml._

import json._
import json.Json._
import text.`UTF-8`
import xml._

import aio._
import aio.Iteratees._
import http.ContentType
import http.MimeType._
import http.Entity
import http.Entity._

object Matching {

  type FromArray[A] = (ArrayEntity ⇒ A)

  val asArray: FromArray[Array[Byte]] = (entity: ArrayEntity) ⇒ entity.array

  val asString: FromArray[String] = (entity: ArrayEntity) ⇒ new String(entity.array, entity.contenttype.charsetOrDefault)

  val asForm: FromArray[Map[String, String]] = (entity: ArrayEntity) ⇒ Map.empty

  val asMultipartForm: FromArray[Map[String, Object]] = (entity: ArrayEntity) ⇒ Map.empty

  val asJson: FromArray[Json] = (entity: ArrayEntity) ⇒ Json.parse(asString(entity))

  val asJObject: FromArray[JObject] = (entity: ArrayEntity) ⇒ asJson(entity).asObject

  val asJArray: FromArray[JArray] = (entity: ArrayEntity) ⇒ asJson(entity).asArray

  val asXml: FromArray[Elem] = (entity: ArrayEntity) ⇒ XML.loadString(asString(entity))

  def asJsonMarshaled[A <: JsonMarshaled](implicit c: ClassTag[A]): FromArray[A] = (entity: ArrayEntity) ⇒ JsonMarshaled[A](asString(entity))

  def asXmlMarshaled[A <: XmlMarshaled](implicit c: ClassTag[A]): FromArray[A] = (entity: ArrayEntity) ⇒ XmlMarshaled[A](asString(entity))

  private val input = Map(
    ContentType(`application/octet-stream`) -> List((typeOf[Array[Byte]], asArray)).toMap,
    ContentType(`text/plain`) -> List((typeOf[String], asString), (typeOf[Map[String, String]], asForm), (typeOf[Array[Byte]], asArray)).toMap)

  private val output = Map(
    ContentType(`application/octet-stream`) -> List((typeOf[Array[Byte]], asArray)).toMap,
    ContentType(`application/json`) -> List((typeOf[com.ibm.plain.json.Json.JObject], asJObject)).toMap,
    ContentType(`text/plain`) -> List((typeOf[String], asString), (typeOf[Map[String, String]], asForm), (typeOf[Array[Byte]], asArray)).toMap)

  println(input)
  println(output)

}
