package com.ibm

package plain

package xml

import java.io.{ InputStream, OutputStream, Reader, StringReader, StringWriter, Writer }
import java.util.logging.{ Level, Logger }

import javax.xml.bind.{ JAXBContext, Marshaller }

import scala.reflect._
import scala.reflect.runtime.universe._

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

  def apply[A](s: String)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshal(new StringReader(s)).asInstanceOf[A]

  def apply[A](in: InputStream)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshal(in).asInstanceOf[A]

  def apply[A](reader: Reader)(implicit c: ClassTag[A]): A = unmarshaller(c.runtimeClass).unmarshal(reader).asInstanceOf[A]

  @inline private[this] def unmarshaller(expected: Class[_]) = JAXBContext.newInstance(expected).createUnmarshaller

  /**
   * Disable verbose stack traces on xml or json parse errors.
   */
  Logger.getLogger(classOf[com.sun.jersey.json.impl.reader.XmlEventProvider].getName).setLevel(Level.OFF)

}

