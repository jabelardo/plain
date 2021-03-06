package com.ibm

package plain

package json

import java.io.Reader

import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson.Serialization

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.Map
import scala.collection.immutable.List
import scala.collection.immutable.Seq
import scala.language.implicitConversions
import scala.math.BigDecimal.double2bigDecimal

import Helpers.stringToConfiggyString

final case class Json(any: Any) {
  override final def toString = Json.build(any)
  final def asNull = convert[Null](null)
  final def asBoolean: Boolean = any match { case Some(json: Json) ⇒ json.asBoolean case b: Boolean ⇒ b case s: String ⇒ s.toBoolean case Json.WrappedJson(s) ⇒ Json.parse(s).asBoolean case _ ⇒ convert[Boolean](false) }
  final def asInt: Int = any match { case Some(json: Json) ⇒ json.asInt case i: Int ⇒ i case l: Long ⇒ l.toInt case d: Double ⇒ d.toInt case s: String ⇒ s.toInt case Json.WrappedJson(s) ⇒ Json.parse(s).asInt case _ ⇒ convert[Int](0) }
  final def asLong: Long = any match { case Some(json: Json) ⇒ json.asLong case i: Int ⇒ i.toLong case s: String ⇒ s.toLong case Json.WrappedJson(s) ⇒ Json.parse(s).asLong case _ ⇒ convert[Long](0L) }
  final def asDouble: Double = any match { case Some(json: Json) ⇒ json.asDouble case i: Int ⇒ i.toDouble case l: Long ⇒ l.toDouble case s: String ⇒ s.toDouble case Json.WrappedJson(s) ⇒ Json.parse(s).asDouble case _ ⇒ convert[Double](0.0) }
  final def asBigDecimal: BigDecimal = any match { case Some(json: Json) ⇒ json.asBigDecimal case Json.WrappedJson(s) ⇒ Json.parse(s).asBigDecimal case _ ⇒ convert[BigDecimal](0.0) }
  final def asString: String = any match { case Json.WrappedJson(s) ⇒ Json.parse(s).asString case _ ⇒ convert[String]("") }
  final def asArray: Json.JArray = any match { case Json.WrappedJson(s) ⇒ Json.parse(s).asArray case _ ⇒ convert[Json.JArray](List[Json]()) }
  final def asObject: Json.JObject = any match { case Json.WrappedJson(s) ⇒ Json.parse(s).asObject case _ ⇒ convert[Json.JObject](Map[String, Json]()) }
  final def apply(i: Int) = asArray(i)
  final def apply(key: String) = asObject(key)
  final def extract[A](implicit mf: Manifest[A]): A = any match { case Json.WrappedJson(s) ⇒ Json.parse4s(s).extract[A](Json.formats, mf) case _ ⇒ throw new JsonException("Cannot extract, use as<Type> instead : " + any) }
  private def convert[T](default: T): T = try {
    any match {
      case Some(json: Json) ⇒ json.convert[T](default)
      case None ⇒ default
      case null ⇒ null.asInstanceOf[T]
      case a: Any ⇒ a.asInstanceOf[T]
    }
  } catch { case e: Throwable ⇒ throw new JsonException("Conversion failed: " + e.getMessage) }
}

class JsonException(reason: String) extends Exception(reason)

trait JsonSerializable {
  def toJson: Json
}

object JsonConversions {
  implicit def Any2Json(a: Any) = Json(a)
  implicit def Json2Boolean(j: Json) = j.asBoolean
  implicit def Json2Int(j: Json) = j.asInt
  implicit def Json2Long(j: Json) = j.asLong
  implicit def Json2Double(j: Json) = j.asDouble
  implicit def Json2BigDecimal(j: Json) = j.asBigDecimal
  implicit def Json2String(j: Json) = j.asString
  implicit def Json2Array(j: Json) = j.asArray
  implicit def Json2Object(j: Json) = j.asObject
}

object Json {

  implicit val formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JodaTimeSerializers.all

  type JArray = List[Json]

  type JObject = Map[String, Json]

  final def build(any: Any): String = {
    @inline def ascii(s: String) = !(encodeOutput || s.exists { c ⇒ '\u0020' > c || '\u007f' < c })

    def quote(s: String) = "\"" + (if (ascii(s)) s else s.regexSub("""[\u0000-\u001f\u0080-\uffff/\"\\]""".r) { m ⇒
      m.matched.charAt(0) match {
        case '\r' ⇒ "\\r"
        case '\n' ⇒ "\\n"
        case '\t' ⇒ "\\t"
        case '"' ⇒ "\\\""
        case '\\' ⇒ "\\\\"
        case '/' ⇒ "\\/"
        case c ⇒ "\\u%04x" format c.asInstanceOf[Int]
      }
    }) + "\""

    def build0(a: Any): QuotedString = QuotedString(a match {
      case QuotedString(inner) ⇒ inner
      case null ⇒ "null"
      case v: Boolean ⇒ v.toString
      case v: Number ⇒ v.toString
      case list: Seq[_] ⇒ list.map(build0(_).inner).mkString("[", ",", "]")
      case map: Map[_, _] ⇒ (for ((key, value) ← map.iterator) yield { quote(key.toString) + ":" + build0(value).inner }).mkString("{", ",", "}")
      case json: Json ⇒ build0(json.any).toString
      case anyref: AnyRef ⇒ Serialization.write(anyref)
      case v ⇒ throw new JsonException(s"Cannot serialize into JSON format : ${v.getClass.getName}.")
    })

    build0(any).toString
  }

  private def convert(any: Any): Json = any match {
    case m: java.util.Map[_, _] ⇒
      val map = new scala.collection.mutable.HashMap[String, Json]
      m.foreach { case (k, v) ⇒ map += ((k.toString, convert(v))) }
      Json(map)
    case l: java.util.List[_] ⇒
      val list = new scala.collection.mutable.ArrayBuffer[Any](l.size)
      l.foreach { e ⇒ list += convert(e) }
      Json(list.toList)
    case a ⇒ Json(a)
  }

  final def parse4s(s: String): org.json4s.JValue = JsonMethods.parse(s)

  final def parse4s(reader: Reader) = JsonMethods.parse(reader)

  final def parse(s: String): Json = convert((new ObjectMapper).readValue(s, classOf[Any]))

  final def parse(reader: Reader): Json = convert((new ObjectMapper).readValue(reader, classOf[Any]))

  final def wrap(s: String): Json = Json(WrappedJson(s))

  case class WrappedJson(val s: String) extends AnyVal

  object Raw {
    def parseAny(s: String): Any = (new ObjectMapper).readValue(s, classOf[Any])
    def parseAny(reader: Reader): Any = (new ObjectMapper).readValue(reader, classOf[Any])

    type JArray = java.util.List[Any]
    type JObject = java.util.Map[String, Any]

    final class Json(val any: Any) extends AnyVal {
      override def toString = any.toString
      def asNull = any.asInstanceOf[Null]
      def asBoolean = any.asInstanceOf[Boolean]
      def asInt = any.asInstanceOf[Int]
      def asLong = any.asInstanceOf[Long]
      def asDouble = any.asInstanceOf[Double]
      def asBigDecimal = any.asInstanceOf[BigDecimal]
      def asString = any.asInstanceOf[String]
      def asArray = any.asInstanceOf[Raw.JArray]
      def asObject = any.asInstanceOf[Raw.JObject]
    }

    implicit def Any2Json(a: Any) = new Json(a)

    implicit def Any2Null(a: Any): Null = new Json(a).asNull
    implicit def Any2Boolean(a: Any) = new Json(a).asBoolean
    implicit def Any2Int(a: Any) = new Json(a).asInt
    implicit def Any2Long(a: Any) = new Json(a).asLong
    implicit def Any2Double(a: Any) = new Json(a).asDouble
    implicit def Any2BigDecimal(a: Any) = new Json(a).asBigDecimal
    implicit def Any2String(a: Any) = new Json(a).asString
    implicit def Any2Array(a: Any) = new Json(a).asArray
    implicit def Any2Object(a: Any) = new Json(a).asObject
  }
}

private final case class QuotedString(inner: String) extends AnyVal {
  override def toString = inner
}

