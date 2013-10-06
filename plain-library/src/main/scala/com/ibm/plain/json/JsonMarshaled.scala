package com.ibm

package plain

package json

import java.io.{ InputStream, OutputStream, Reader, StringReader, StringWriter, Writer }

import scala.reflect.ClassTag

import com.sun.jersey.api.json.JSONJAXBContext

/**
 * Do not forget to implement a default no-parameters constructor for classes deriving from JsonMarshaled or you will get an InvalidAnnotationException.
 */
trait JsonMarshaled {

  final def toJson(out: OutputStream) = marshaller.marshallToJSON(this, out)

  final def toJson(writer: Writer) = marshaller.marshallToJSON(this, writer)

  final def toJson: String = {
    val writer = new StringWriter
    toJson(writer)
    writer.toString
  }

  def asJson: Json = Json(toJson)

  private[this] def context = new JSONJAXBContext(jsonconfiguration, this.getClass)

  private[this] def marshaller = context.createJSONMarshaller

}

/**
 * Use this to unmarshal from json into objects.
 */
object JsonMarshaled {

  final def apply[A](s: String)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshalFromJSON(new StringReader(s), c.runtimeClass).asInstanceOf[A]

  final def apply[A](in: InputStream)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshalFromJSON(in, c.runtimeClass).asInstanceOf[A]

  final def apply[A](reader: Reader)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshalFromJSON(reader, c.runtimeClass).asInstanceOf[A]

  @inline private[this] def unmarshaller(expected: Class[_]) = new JSONJAXBContext(jsonconfiguration, expected).createJSONUnmarshaller

}

