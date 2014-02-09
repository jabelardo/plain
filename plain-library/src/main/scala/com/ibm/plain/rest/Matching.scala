package com.ibm

package plain

package rest

import java.nio.{ ByteBuffer, CharBuffer }
import java.util.Arrays.copyOfRange
import scala.collection.mutable.{ HashMap, MultiMap, Set ⇒ MutableSet }
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{ Type, typeOf }
import scala.xml.{ XML, Elem ⇒ Xml }
import scala.language.implicitConversions
import json.{ Json, JsonMarshaled }
import json.Json.{ JArray, JObject }
import text.{ `UTF-8`, fastSplit, convertCharset }
import xml.XmlMarshaled
import aio.tooTinyToCareSize
import http.{ defaultCharacterSet, defaultcodec }
import http.Status.{ ClientError, ServerError }
import http.ContentType
import http.MimeType
import http.MimeType._
import http.Entity
import http.Entity._

final class Matching {

  import Matching._

  object Types {
    final val entity = typeOf[Entity]
    final val unit = typeOf[Unit]
    final val array = typeOf[Array[Byte]]
    final val bytebuffer = typeOf[ByteBuffer]
    final val string = typeOf[String]
    final val html = typeOf[Html]
    final val form = typeOf[Form]
    final val multipartform = typeOf[MultipartForm]
    final val json = typeOf[Json]
    final val jobject = typeOf[JObject]
    final val jarray = typeOf[JArray]
    final val jsonmarshaled = typeOf[JsonMarshaled]
    final val xml = typeOf[Xml]
    final val xmlmarshaled = typeOf[XmlMarshaled]
  }

  import Types._

  final val decodeEntity: Decoder[Entity] = (entity: Option[Entity]) ⇒ entity match { case Some(entity) ⇒ entity case _ ⇒ throw ClientError.`415` }

  final val decodeUnit: Decoder[Unit] = (entity: Option[Entity]) ⇒ ()

  final val decodeArray: Decoder[Array[Byte]] = (entity: Option[Entity]) ⇒ entity match { case Some(a: ArrayEntity) ⇒ copyOfRange(a.array, a.offset, a.length.toInt) case _ ⇒ throw ClientError.`415` }

  final val decodeString: Decoder[String] = (entity: Option[Entity]) ⇒ entity match { case Some(a: ArrayEntity) ⇒ new String(a.array, a.offset, a.length.toInt, a.contenttype.charsetOrDefault) case _ ⇒ throw ClientError.`415` }

  final val decodeHtml: Decoder[Html] = (entity: Option[Entity]) ⇒ new Html(XML.loadString(decodeString(entity)))

  final val decodeForm: Decoder[Form] = (entity: Option[Entity]) ⇒ {
    @inline def c(s: String) = convertCharset(defaultcodec.decode(s), defaultCharacterSet, `UTF-8`)
    val form = fastSplit(decodeString(entity), '&')
      .map(fastSplit(_, '='))
      .collect { case List(k, v) ⇒ (c(k), c(v)) }
      .foldLeft(new HashMap[String, MutableSet[String]] with MultiMap[String, String]) { case (l, (k, v)) ⇒ l.addBinding(k, v) }
    form
  }

  final val decodeMultipartForm: Decoder[MultipartForm] = (entity: Option[Entity]) ⇒ Map("foo" -> "bar")

  final val decodeJson: Decoder[Json] = (entity: Option[Entity]) ⇒ Json.parse(decodeString(entity))

  final val decodeJArray: Decoder[JArray] = (entity: Option[Entity]) ⇒ decodeJson(entity).asArray

  final val decodeJObject: Decoder[JObject] = (entity: Option[Entity]) ⇒ decodeJson(entity).asObject

  final val decodeXml: Decoder[Xml] = (entity: Option[Entity]) ⇒ XML.loadString(decodeString(entity))

  def decodeJsonMarshaled[A <: JsonMarshaled]: MarshaledDecoder[A] = (entity: Option[Entity], c: ClassTag[A]) ⇒ JsonMarshaled[A](decodeString(entity))(c)

  def decodeXmlMarshaled[A <: XmlMarshaled]: MarshaledDecoder[A] = (entity: Option[Entity], c: ClassTag[A]) ⇒ XmlMarshaled[A](decodeString(entity))(c)

  final val decoders: TypeDecoders = Array(
    (entity, decodeEntity),
    (unit, decodeUnit),
    (array, decodeArray),
    (string, decodeString),
    (html, decodeHtml),
    (form, decodeForm),
    (multipartform, decodeMultipartForm),
    (json, decodeJson),
    (jobject, decodeJObject),
    (jarray, decodeJArray),
    (xml, decodeXml),
    (jsonmarshaled, decodeJsonMarshaled),
    (xmlmarshaled, decodeXmlMarshaled)).toMap

  final val encodeEntity: Encoder = ((entity: Entity) ⇒ Some(entity)).asInstanceOf[Encoder]

  final val encodeUnit: Encoder = ((u: Unit) ⇒ None).asInstanceOf[Encoder]

  final val encodeArray: Encoder = ((array: Array[Byte]) ⇒ Some(ArrayEntity(array, `text/plain`))).asInstanceOf[Encoder]

  final val encodeByteBuffer: Encoder = ((buffer: ByteBuffer) ⇒ Some(ByteBufferEntity(buffer, `application/octet-stream`))).asInstanceOf[Encoder]

  final val encodeString: Encoder = ((s: String) ⇒
    Some(if (tooTinyToCareSize < s.length) ByteBufferEntity(s, ContentType(`text/plain`, `UTF-8`)) else ArrayEntity(s.getBytes(`UTF-8`), ContentType(`text/plain`, `UTF-8`)))).asInstanceOf[Encoder]

  final val encodeHtml: Encoder = ((html: Html) ⇒ Some(ByteBufferEntity("<!DOCTYPE html>" + html.xml.buildString(true), `text/html`))).asInstanceOf[Encoder]

  final val encodeForm: Encoder = ((form: Form) ⇒ {
    @inline def c(s: String) = defaultcodec.encode(convertCharset(s, `UTF-8`, defaultCharacterSet))
    val f = form.foldLeft(new StringBuilder) { case (l, (k, values)) ⇒ values.foreach(value ⇒ l.append(c(k)).append('=').append(c(value)).append('&')); l }
    Some(ByteBufferEntity(f.stripSuffix("&"), `application/x-www-form-urlencoded`))
  }).asInstanceOf[Encoder]

  final val encodeMultipartForm: Encoder = ((form: MultipartForm) ⇒ Some(ArrayEntity(Array[Byte](), `multipart/form-data`))).asInstanceOf[Encoder]

  final val encodeJson: Encoder = ((json: Json) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  final val encodeJObject: Encoder = ((json: JObject) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  final val encodeJArray: Encoder = ((json: JArray) ⇒ Some(ByteBufferEntity(Json.build(json), `application/json`))).asInstanceOf[Encoder]

  final val encodeXml: Encoder = ((xml: Xml) ⇒ Some(ByteBufferEntity(xml.buildString(true), `application/xml`))).asInstanceOf[Encoder]

  final val encodeJsonMarshaled: Encoder = ((marshaled: JsonMarshaled) ⇒ Some(ByteBufferEntity(marshaled.toJson, `application/json`))).asInstanceOf[Encoder]

  final val encodeXmlMarshaled: Encoder = ((marshaled: XmlMarshaled) ⇒ Some(ByteBufferEntity(marshaled.toXml, `application/xml`))).asInstanceOf[Encoder]

  final val encoders: TypeEncoders = Array(
    (entity, encodeEntity),
    (unit, encodeUnit),
    (array, encodeArray),
    (bytebuffer, encodeByteBuffer),
    (string, encodeString),
    (html, encodeHtml),
    (form, encodeForm),
    (multipartform, encodeMultipartForm),
    (json, encodeJson),
    (jobject, encodeJObject),
    (jarray, encodeJArray),
    (xml, encodeXml),
    (jsonmarshaled, encodeJsonMarshaled),
    (xmlmarshaled, encodeXmlMarshaled)).toMap

  final val inputPriority: PriorityList = Array(
    (`application/x-scala-unit`, List(unit, entity)),
    (`application/octet-stream`, List(array, entity)),
    (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, array, entity)),
    (`application/xml`, List(xmlmarshaled, xml, string, array, entity)),
    (`application/x-www-form-urlencoded`, List(form, string, array, entity)),
    (`multipart/form-data`, List(multipartform, string, array, entity)),
    (`text/plain`, List(xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, form, string, array, entity)))

  final val outputPriority: PriorityList = Array(
    (`application/x-scala-unit`, List(unit, entity)),
    (`application/octet-stream`, List(bytebuffer, array, entity, unit)),
    (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, bytebuffer, array, entity, unit)),
    (`application/xml`, List(xmlmarshaled, xml, string, bytebuffer, array, entity, unit)),
    (`application/xhtml+xml`, List(xml, string, bytebuffer, array, entity, unit)),
    (`application/x-www-form-urlencoded`, List(form, string, bytebuffer, array, entity, unit)),
    (`multipart/form-data`, List(multipartform, string, bytebuffer, array, entity, unit)),
    (`text/html`, List(html, string, xml, bytebuffer, array, entity, unit)),
    (`text/plain`, List(string, form, jsonmarshaled, xmlmarshaled, xml, jobject, jarray, json, bytebuffer, array, entity, unit)),
    (`*/*`, List(html, string, form, jsonmarshaled, xmlmarshaled, xml, jobject, jarray, json, bytebuffer, array, entity, unit)))

  final val priorities: Array[((MimeType, MimeType), (Type, Type))] = for {
    (inmimetype, intypelist) ← inputPriority
    intype ← intypelist
    (outmimetype, outtypelist) ← outputPriority
    outtype ← outtypelist
  } yield ((inmimetype, outmimetype), (intype, outtype))

}

/**
 *
 */
object Matching {

  type Decoder[A] = Option[Entity] ⇒ A

  type MarshaledDecoder[A] = (Option[Entity], ClassTag[A]) ⇒ A

  type Encoder = Any ⇒ Option[Entity]

  type TypeDecoders = Map[Type, AnyRef]

  type TypeEncoders = Map[Type, Encoder]

  type PriorityList = Array[(MimeType, List[Type])]

  final val default = new Matching

}
