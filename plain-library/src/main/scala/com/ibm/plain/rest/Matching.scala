package com.ibm

package plain

package rest

import java.nio.{ ByteBuffer, CharBuffer }

import org.apache.commons.codec.net.URLCodec

import scala.collection.mutable.{ HashMap, MultiMap, Set ⇒ MutableSet }
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{ Type, typeOf }
import scala.xml.{ XML, Elem ⇒ Xml }

import json.{ Json, JsonMarshaled }
import json.Json.{ JArray, JObject }
import text.{ `UTF-8`, fastSplit, convertCharset }
import xml.XmlMarshaled

import http.defaultCharacterSet
import http.Status.{ ClientError, ServerError }
import http.ContentType
import http.MimeType
import http.MimeType._
import http.Entity
import http.Entity._

private final class Matching {

  import Matching._

  object Types {
    val entity = typeOf[Entity]
    val unit = typeOf[Unit]
    val array = typeOf[Array[Byte]]
    val bytebuffer = typeOf[ByteBuffer]
    val string = typeOf[String]
    val form = typeOf[Form]
    val multipart = typeOf[MultipartForm]
    val json = typeOf[Json]
    val jobject = typeOf[JObject]
    val jarray = typeOf[JArray]
    val jsonmarshaled = typeOf[JsonMarshaled]
    val xml = typeOf[Xml]
    val xmlmarshaled = typeOf[XmlMarshaled]
  }

  import Types._

  private[this] final val codec = new URLCodec(defaultCharacterSet.toString)

  val decodeEntity: Decoder[Entity] = (entity: Option[Entity]) ⇒ entity match { case Some(entity) ⇒ entity case _ ⇒ throw ClientError.`415` }

  val decodeUnit: Decoder[Unit] = (entity: Option[Entity]) ⇒ ()

  val decodeArray: Decoder[Array[Byte]] = (entity: Option[Entity]) ⇒ entity match { case Some(a: ArrayEntity) ⇒ a.array case _ ⇒ throw ClientError.`415` }

  val decodeString: Decoder[String] = (entity: Option[Entity]) ⇒ entity match { case Some(a: ArrayEntity) ⇒ new String(a.array, a.contenttype.charsetOrDefault) case _ ⇒ throw ClientError.`415` }

  val decodeForm: Decoder[Form] = (entity: Option[Entity]) ⇒ {
    @inline def c(s: String) = convertCharset(codec.decode(s), defaultCharacterSet, `UTF-8`)
    fastSplit(decodeString(entity), '&')
      .map(fastSplit(_, '='))
      .collect { case List(k, v) ⇒ (c(k), c(v)) }
      .foldLeft(new HashMap[String, MutableSet[String]] with MultiMap[String, String]) { case (l, (k, v)) ⇒ l.addBinding(k, v) }
  }

  val decodeMultipartForm: Decoder[MultipartForm] = (entity: Option[Entity]) ⇒ Map("foo" -> "bar")

  val decodeJson: Decoder[Json] = (entity: Option[Entity]) ⇒ Json.parse(decodeString(entity))

  val decodeJArray: Decoder[JArray] = (entity: Option[Entity]) ⇒ decodeJson(entity).asArray

  val decodeJObject: Decoder[JObject] = (entity: Option[Entity]) ⇒ decodeJson(entity).asObject

  val decodeXml: Decoder[Xml] = (entity: Option[Entity]) ⇒ XML.loadString(decodeString(entity))

  def decodeJsonMarshaled[A <: JsonMarshaled]: MarshaledDecoder[A] = (entity: Option[Entity], c: ClassTag[A]) ⇒ JsonMarshaled[A](decodeString(entity))(c)

  def decodeXmlMarshaled[A <: XmlMarshaled]: MarshaledDecoder[A] = (entity: Option[Entity], c: ClassTag[A]) ⇒ XmlMarshaled[A](decodeString(entity))(c)

  val decoders: TypeDecoders = Array(
    (typeOf[Entity], decodeEntity),
    (typeOf[Unit], decodeUnit),
    (typeOf[Array[Byte]], decodeArray),
    (typeOf[String], decodeString),
    (typeOf[Form], decodeForm),
    (typeOf[MultipartForm], decodeMultipartForm),
    (typeOf[Json], decodeJson),
    (typeOf[JArray], decodeJArray),
    (typeOf[JObject], decodeJObject),
    (typeOf[JsonMarshaled], decodeJsonMarshaled),
    (typeOf[Xml], decodeXml),
    (typeOf[XmlMarshaled], decodeXmlMarshaled)).toMap

  val encodeEntity: Encoder = ((entity: Entity) ⇒ Some(entity)).asInstanceOf[Encoder]

  val encodeUnit: Encoder = ((u: Unit) ⇒ None).asInstanceOf[Encoder]

  val encodeArray: Encoder = ((array: Array[Byte]) ⇒ Some(ArrayEntity(array, `application/octet-stream`))).asInstanceOf[Encoder]

  val encodeByteBuffer: Encoder = ((buffer: ByteBuffer) ⇒ Some(ByteBufferEntity(buffer, `application/octet-stream`))).asInstanceOf[Encoder]

  val encodeString: Encoder = ((s: String) ⇒ Some(ByteBufferEntity(s, ContentType(`text/plain`, `UTF-8`)))).asInstanceOf[Encoder]

  val encodeForm: Encoder = ((form: Form) ⇒ {
    @inline def c(s: String) = codec.encode(convertCharset(s, `UTF-8`, defaultCharacterSet))
    val f = form.foldLeft(new StringBuilder) { case (l, (k, values)) ⇒ values.foreach(value ⇒ l.append(c(k)).append('=').append(c(value)).append('&')); l }
    Some(ByteBufferEntity(f.stripSuffix("&"), `application/x-www-form-urlencoded`))
  }).asInstanceOf[Encoder]

  val encodeMultipartForm: Encoder = ((form: MultipartForm) ⇒ Some(ArrayEntity(Array[Byte](), `multipart/form-data`))).asInstanceOf[Encoder]

  val encodeJson: Encoder = ((json: Json) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  val encodeJObject: Encoder = ((json: JObject) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  val encodeJArray: Encoder = ((json: JArray) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  val encodeXml: Encoder = ((xml: Xml) ⇒ Some(ByteBufferEntity(xml.buildString(true), `application/xml`))).asInstanceOf[Encoder]

  val encodeJsonMarshaled: Encoder = ((marshaled: JsonMarshaled) ⇒ Some(ByteBufferEntity(marshaled.toJson, `application/json`))).asInstanceOf[Encoder]

  val encodeXmlMarshaled: Encoder = ((marshaled: XmlMarshaled) ⇒ Some(ByteBufferEntity(marshaled.toXml, `application/xml`))).asInstanceOf[Encoder]

  val encoders: TypeEncoders = Array(
    (typeOf[Entity], encodeEntity),
    (typeOf[Unit], encodeUnit),
    (typeOf[Array[Byte]], encodeArray),
    (typeOf[ByteBuffer], encodeByteBuffer),
    (typeOf[String], encodeString),
    (typeOf[Form], encodeForm),
    (typeOf[MultipartForm], encodeMultipartForm),
    (typeOf[Json], encodeJson),
    (typeOf[JObject], encodeJObject),
    (typeOf[JArray], encodeJArray),
    (typeOf[Xml], encodeXml),
    (typeOf[JsonMarshaled], encodeJsonMarshaled),
    (typeOf[XmlMarshaled], encodeXmlMarshaled)).toMap

  val inputPriority: PriorityList = Array(
    (`application/x-scala-unit`, List(unit, entity)),
    (`application/octet-stream`, List(array, entity)),
    (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, array, entity)),
    (`application/xml`, List(xmlmarshaled, xml, string, array, entity)),
    (`application/x-www-form-urlencoded`, List(form, string, array, entity)),
    (`multipart/form-data`, List(multipart, string, array, entity)),
    (`text/plain`, List(string, form, xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, array, entity)))

  val outputPriority: PriorityList = Array(
    (`application/x-scala-unit`, List(unit, entity)),
    (`application/octet-stream`, List(bytebuffer, array, entity)),
    (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, bytebuffer, array, entity)),
    (`application/xml`, List(xmlmarshaled, xml, string, bytebuffer, array, entity)),
    (`application/xhtml+xml`, List(xml, string, bytebuffer, array, entity)),
    (`application/x-www-form-urlencoded`, List(form, string, bytebuffer, array, entity)),
    (`multipart/form-data`, List(multipart, string, bytebuffer, array, entity)),
    (`text/html`, List(string, xml, bytebuffer, array, entity)),
    (`text/plain`, List(string, form, jsonmarshaled, xmlmarshaled, xml, jobject, jarray, json, bytebuffer, array, entity)),
    (`*/*`, List(string, form, jsonmarshaled, xmlmarshaled, xml, jobject, jarray, json, bytebuffer, array, entity)))

  val priorities: Array[((MimeType, MimeType), (Type, Type))] = for {
    (inmimetype, intypelist) ← inputPriority
    intype ← intypelist
    (outmimetype, outtypelist) ← outputPriority
    outtype ← outtypelist
  } yield ((inmimetype, outmimetype), (intype, outtype))

}

private object Matching {

  type Decoder[A] = Option[Entity] ⇒ A

  type MarshaledDecoder[A] = (Option[Entity], ClassTag[A]) ⇒ A

  type Encoder = Any ⇒ Option[Entity]

  type TypeDecoders = Map[Type, AnyRef]

  type TypeEncoders = Map[Type, Encoder]

  type PriorityList = Array[(MimeType, List[Type])]

}
