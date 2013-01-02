package com.ibm

package plain

package rest

import java.nio.charset.Charset

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.xml._

import json._
import json.Json._
import text._
import xml._

import aio._
import aio.Iteratees._
import http.Status.ClientError
import http.ContentType
import http.MimeType
import http.MimeType._
import http.Entity
import http.Entity._

object Matching {

  type ArrayEntityDecoder[A] = ArrayEntity ⇒ A

  type ArrayEntityMarshaledDecoder[A] = (ArrayEntity, ClassTag[A]) ⇒ A

  trait MarshaledDecoder

  private[this] val asArray: ArrayEntityDecoder[Array[Byte]] = (entity: ArrayEntity) ⇒ entity.array

  private[this] val asString: ArrayEntityDecoder[String] = (entity: ArrayEntity) ⇒ new String(entity.array, entity.contenttype.charsetOrDefault)

  private[this] val asForm: ArrayEntityDecoder[Map[String, String]] = (entity: ArrayEntity) ⇒ Map("foo" -> "bar")

  private[this] val asMultipartForm: ArrayEntityDecoder[Map[String, Object]] = (entity: ArrayEntity) ⇒ Map.empty

  private[this] val asJson: ArrayEntityDecoder[Json] = (entity: ArrayEntity) ⇒ Json.parse(asString(entity))

  private[this] val asJObject: ArrayEntityDecoder[JObject] = (entity: ArrayEntity) ⇒ asJson(entity).asObject

  private[this] val asJArray: ArrayEntityDecoder[JArray] = (entity: ArrayEntity) ⇒ asJson(entity).asArray

  private[this] val asXml: ArrayEntityDecoder[Elem] = (entity: ArrayEntity) ⇒ XML.loadString(asString(entity))

  private[this] def asJsonMarshaled[A <: JsonMarshaled]: ArrayEntityMarshaledDecoder[A] = (entity: ArrayEntity, c: ClassTag[A]) ⇒ JsonMarshaled[A](asString(entity))(c)

  private[this] def asXmlMarshaled[A <: XmlMarshaled]: ArrayEntityMarshaledDecoder[A] = (entity: ArrayEntity, c: ClassTag[A]) ⇒ XmlMarshaled[A](asString(entity))(c)

  private type ArrayEntityEncoder[A] = (A ⇒ ArrayEntity)

  private[this] val fromArray: ArrayEntityEncoder[Array[Byte]] = (array: Array[Byte]) ⇒ ArrayEntity(array, `application/octet-stream`)

  private[this] val fromString: ArrayEntityEncoder[String] = (s: String) ⇒ ArrayEntity(s.getBytes(`ISO-8859-15`), `text/plain`)

  private[this] val fromForm: ArrayEntityEncoder[Map[String, String]] = (form: Map[String, String]) ⇒ ArrayEntity(null, `application/x-www-form-urlencoded`)

  private[this] val fromMultipartForm: ArrayEntityEncoder[Map[String, Object]] = (form: Map[String, Object]) ⇒ ArrayEntity(null, `multipart/form-data`)

  private[this] val fromJson: ArrayEntityEncoder[Json] = (json: Json) ⇒ ArrayEntity(Json.build(json).getBytes(`UTF-8`), `application/json`)

  private[this] val fromXml: ArrayEntityEncoder[Elem] = (elem: Elem) ⇒ ArrayEntity(elem.buildString(true).getBytes(`UTF-8`), `application/xml`)

  private[this] val fromJsonMarshaled: ArrayEntityEncoder[JsonMarshaled] = (marshaled: JsonMarshaled) ⇒ ArrayEntity(marshaled.toJson.getBytes(`UTF-8`), `application/json`)

  private[this] val fromXmlMarshaled: ArrayEntityEncoder[XmlMarshaled] = (marshaled: XmlMarshaled) ⇒ ArrayEntity(marshaled.toXml.getBytes(`UTF-8`), `application/xml`)

  private[rest] val In: Map[MimeType, List[(Type, AnyRef)]] = {
    val array = (typeOf[Array[Byte]], asArray)
    val string = (typeOf[String], asString)
    val form = (typeOf[Map[String, String]], asForm)
    val multipart = (typeOf[Map[String, Object]], asMultipartForm)
    val json = (typeOf[Json], asJson)
    val jobject = (typeOf[JObject], asJObject)
    val jarray = (typeOf[JArray], asJArray)
    val jsonmarshaled = (typeOf[JsonMarshaled], asJsonMarshaled)
    val xml = (typeOf[Elem], asXml)
    val xmlmarshaled = (typeOf[XmlMarshaled], asXmlMarshaled)
    Map(
      `application/octet-stream` -> List(array),
      `application/json` -> List(jsonmarshaled, jobject, jarray, json, string, array),
      `application/xml` -> List(xmlmarshaled, xml, string, array),
      `application/x-www-form-urlencoded` -> List(form, string, array),
      `multipart/form-data` -> List(multipart, string, array),
      `text/plain` -> List(string, form, xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, array))
  }

}
