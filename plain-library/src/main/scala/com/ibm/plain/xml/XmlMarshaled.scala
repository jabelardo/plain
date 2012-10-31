package com.ibm

package plain

package xml

import java.io.{ InputStream, OutputStream, Reader, StringReader, StringWriter, Writer }

import javax.xml.bind.{ JAXBContext, Marshaller }

/**
 * Do not forget to implement a default no-parameters constructor for classes deriving from XmlMarshaled or you will get InvalidAnnotationExceptions.
 */
trait XmlMarshaled {

  def toXml(out: OutputStream) = marshaller.marshal(this, out)

  def toXml(writer: Writer) = marshaller.marshal(this, writer)

  def toXml: String = {
    val writer = new StringWriter
    toXml(writer)
    writer.toString
  }

  /**
   * Do not try to make this a lazy val. Java reflection will call it during newInstance and fail.
   */
  private[this] def context = JAXBContext.newInstance(this.getClass)

  private[this] def marshaller = {
    val m = context.createMarshaller
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput)
    m.setProperty("com.sun.xml.bind.characterEscapeHandler", CDataEscapeHandler)
    m
  }

}

/**
 * Use this to unmarshal from xml into objects.
 */
object XmlMarshaled {

  def apply(s: String, expected: Class[_]) = unmarshaller(expected).unmarshal(new StringReader(s))

  def apply(in: InputStream, expected: Class[_]) = unmarshaller(expected).unmarshal(in)

  def apply(reader: Reader, expected: Class[_]) = unmarshaller(expected).unmarshal(reader)

  private[this] def unmarshaller(expected: Class[_]) = JAXBContext.newInstance(expected).createUnmarshaller

}

