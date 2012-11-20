package com.ibm

package plain

package json

import java.io.{ InputStream, OutputStream, Reader, StringReader, StringWriter, Writer }

import com.sun.jersey.api.json.JSONJAXBContext

/**
 * Do not forget to implement a default no-parameters constructor for classes deriving from JsonMarshaled or you will get an InvalidAnnotationException.
 */
trait JsonMarshaled {

  def toJson(out: OutputStream) = marshaller.marshallToJSON(this, out)

  def toJson(writer: Writer) = marshaller.marshallToJSON(this, writer)

  def toJson: String = {
    val writer = new StringWriter
    toJson(writer)
    writer.toString
  }

  private[this] def context = new JSONJAXBContext(jsonconfiguration, this.getClass)

  private[this] def marshaller = context.createJSONMarshaller

}

/**
 * Use this to unmarshal from json into objects.
 */
object JsonMarshaled {

  def apply(s: String, expected: Class[_]) = unmarshaller(expected).unmarshalFromJSON(new StringReader(s), expected)

  def apply(in: InputStream, expected: Class[_]) = unmarshaller(expected).unmarshalFromJSON(in, expected)

  def apply(reader: Reader, expected: Class[_]) = unmarshaller(expected).unmarshalFromJSON(reader, expected)

  private[this] def unmarshaller(expected: Class[_]) = new JSONJAXBContext(jsonconfiguration, expected).createJSONUnmarshaller

}

