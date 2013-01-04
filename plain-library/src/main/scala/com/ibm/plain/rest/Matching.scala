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

  val asArray: ArrayEntityDecoder[Array[Byte]] = (entity: ArrayEntity) ⇒ entity.array

  val asString: ArrayEntityDecoder[String] = (entity: ArrayEntity) ⇒ new String(entity.array, entity.contenttype.charsetOrDefault)

  val asForm: ArrayEntityDecoder[Map[String, String]] = (entity: ArrayEntity) ⇒ Map("foo" -> "bar")

  val asMultipartForm: ArrayEntityDecoder[Map[String, Object]] = (entity: ArrayEntity) ⇒ Map.empty

  val asJson: ArrayEntityDecoder[Json] = (entity: ArrayEntity) ⇒ Json.parse(asString(entity))

  val asJObject: ArrayEntityDecoder[JObject] = (entity: ArrayEntity) ⇒ asJson(entity).asObject

  val asJArray: ArrayEntityDecoder[JArray] = (entity: ArrayEntity) ⇒ asJson(entity).asArray

  val asXml: ArrayEntityDecoder[Elem] = (entity: ArrayEntity) ⇒ XML.loadString(asString(entity))

  def asJsonMarshaled[A <: JsonMarshaled]: ArrayEntityMarshaledDecoder[A] = (entity: ArrayEntity, c: ClassTag[A]) ⇒ JsonMarshaled[A](asString(entity))(c)

  def asXmlMarshaled[A <: XmlMarshaled]: ArrayEntityMarshaledDecoder[A] = (entity: ArrayEntity, c: ClassTag[A]) ⇒ XmlMarshaled[A](asString(entity))(c)

  private type ArrayEntityEncoder[A] = (A ⇒ ArrayEntity)

  val fromArray: ArrayEntityEncoder[Array[Byte]] = (array: Array[Byte]) ⇒ ArrayEntity(array, `application/octet-stream`)

  val fromString: ArrayEntityEncoder[String] = (s: String) ⇒ ArrayEntity(s.getBytes(`ISO-8859-15`), `text/plain`)

  val fromForm: ArrayEntityEncoder[Map[String, String]] = (form: Map[String, String]) ⇒ ArrayEntity(null, `application/x-www-form-urlencoded`)

  val fromMultipartForm: ArrayEntityEncoder[Map[String, Object]] = (form: Map[String, Object]) ⇒ ArrayEntity(null, `multipart/form-data`)

  val fromJson: ArrayEntityEncoder[Json] = (json: Json) ⇒ ArrayEntity(Json.build(json).getBytes(`UTF-8`), `application/json`)

  val fromXml: ArrayEntityEncoder[Elem] = (elem: Elem) ⇒ ArrayEntity(elem.buildString(true).getBytes(`UTF-8`), `application/xml`)

  val fromJsonMarshaled: ArrayEntityEncoder[JsonMarshaled] = (marshaled: JsonMarshaled) ⇒ ArrayEntity(marshaled.toJson.getBytes(`UTF-8`), `application/json`)

  val fromXmlMarshaled: ArrayEntityEncoder[XmlMarshaled] = (marshaled: XmlMarshaled) ⇒ ArrayEntity(marshaled.toXml.getBytes(`UTF-8`), `application/xml`)

  val In: List[(MimeType, List[(Type, AnyRef)])] = {
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
    List(
      (`application/octet-stream`, List(array)),
      (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, array)),
      (`application/xml`, List(xmlmarshaled, xml, string, array)),
      (`application/x-www-form-urlencoded`, List(form, string, array)),
      (`multipart/form-data`, List(multipart, string, array)),
      (`text/plain`, List(string, form, xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, array)))
  }

  object Types {
    val entity = typeOf[Entity]
    val unit = typeOf[Unit]
    val array = typeOf[Array[Byte]]
    val string = typeOf[String]
    val form = typeOf[Map[String, String]]
    val multipart = typeOf[Map[String, Object]]
    val json = typeOf[Json]
    val jobject = typeOf[JObject]
    val jarray = typeOf[JArray]
    val jsonmarshaled = typeOf[JsonMarshaled]
    val xml = typeOf[Elem]
    val xmlmarshaled = typeOf[XmlMarshaled]
  }

  import Types._

  type Decoder[A] = (Entity ⇒ A)

  type Encoder = (Any ⇒ Entity)

  type TypeDecoders = Map[Type, Decoder[Any]]

  type TypeEncoders = Map[Type, Encoder]

  val decodeEntity: Decoder[Entity] = (entity: Entity) ⇒ entity

  val decodeUnit: Decoder[Unit] = (entity: Entity) ⇒ ()

  val decodeArray: Decoder[Array[Byte]] = (entity: Entity) ⇒ entity match { case a: ArrayEntity ⇒ a.array case _ ⇒ throw ClientError.`415` }

  val decodeString: Decoder[String] = (entity: Entity) ⇒ entity match { case a: ArrayEntity ⇒ new String(a.array, a.contenttype.charsetOrDefault) case _ ⇒ throw ClientError.`415` }

  val inputDecoders: TypeDecoders = Map(
    typeOf[Entity] -> decodeEntity,
    typeOf[Unit] -> decodeUnit,
    typeOf[Array[Byte]] -> decodeArray,
    typeOf[String] -> decodeString)

  val encodeEntity: Encoder = ((entity: Entity) ⇒ entity).asInstanceOf[Encoder]

  val encodeUnit: Encoder = null

  val encodeArray: Encoder = ((array: Array[Byte]) ⇒ ArrayEntity(array, `application/octet-stream`)).asInstanceOf[Encoder]

  val encodeString: Encoder = ((s: String) ⇒ ArrayEntity(s.getBytes(`ISO-8859-15`), `text/plain`)).asInstanceOf[Encoder]

  val outputEncoders: TypeEncoders = Map(
    typeOf[Entity] -> encodeEntity,
    typeOf[Unit] -> encodeUnit,
    typeOf[Array[Byte]] -> encodeArray,
    typeOf[String] -> encodeString)

  type PriorityList = List[(MimeType, List[Type])]

  val inputPriority: PriorityList =
    List(
      (`application/x-scala-unit`, List(unit, entity)),
      (`application/octet-stream`, List(array, entity)),
      (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, array, entity)),
      (`application/xml`, List(xmlmarshaled, xml, string, array, entity)),
      (`application/x-www-form-urlencoded`, List(form, string, array, entity)),
      (`multipart/form-data`, List(multipart, string, array, entity)),
      (`text/plain`, List(string, form, xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, array, entity)))

  val outputPriority: PriorityList =
    List(
      (`application/x-scala-unit`, List(unit, entity)),
      (`application/octet-stream`, List(array, entity)),
      (`application/json`, List(jsonmarshaled, jobject, jarray, json, string, array, entity)),
      (`application/xml`, List(xmlmarshaled, xml, string, array, entity)),
      (`application/x-www-form-urlencoded`, List(form, string, array, entity)),
      (`multipart/form-data`, List(multipart, string, array, entity)),
      (`text/plain`, List(string, form, xmlmarshaled, jsonmarshaled, xml, jobject, jarray, json, array, entity)))

  val priorities: List[((MimeType, MimeType), (Type, Type))] = for {
    (inmimetype, intypelist) ← inputPriority
    intype ← intypelist
    (outmimetype, outtypelist) ← outputPriority
    outtype ← outtypelist
  } yield ((inmimetype, outmimetype), (intype, outtype))

  println("prios " + priorities.size)

  val resourcepriorities = priorities.filter { case (_, inout) ⇒ methodbodies.unzip._1.contains(inout) }

  println("resourceprios " + resourcepriorities.size + " " + resourcepriorities)

  val inmimetype: MimeType = `text/plain`

  val outmimetypes: List[MimeType] = List(`text/plain`, `application/octet-stream`, `application/x-scala-unit`)

  type MethodBodies = List[((Type, Type), Any ⇒ Any)]

  lazy val methodbodies: MethodBodies = ((typeOf[Unit], typeOf[Unit]), (a: Any) ⇒ ()) :: ((typeOf[String], typeOf[String]), (s: Any) ⇒ s.toString.reverse) :: Nil

  val inentity = ArrayEntity("This is a string.".getBytes, `text/plain`)

  println(methodbodies)
  println(inentity)

  val out = outmimetypes.collectFirst {
    case outmimetype ⇒ resourcepriorities.collectFirst {
      case (inoutmimetype, inouttype) if inoutmimetype == (inmimetype, outmimetype) ⇒
        println("here 1 " + inoutmimetype + " " + inouttype); methodbodies.collectFirst {
          case ((in, out), methodbody) if (in, out) == inouttype ⇒ println("here 2 " + (in, out) + " " + inouttype); (methodbody, inputDecoders.get(in), outputEncoders.get(out))
        }
    } match {
      case r @ Some(Some((methodbody, Some(decode), Some(encode)))) ⇒
        println("here3 " + r); methodbody(decode(inentity))
      case _ ⇒ println("here 4"); None
    }
  } match {
    case Some(outentity) ⇒ outentity
    case _ ⇒ "no out entity"
  }

  println(out)

}
