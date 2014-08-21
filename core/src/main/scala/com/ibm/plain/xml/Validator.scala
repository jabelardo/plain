package com.ibm

package plain

package xml

import java.io.InputStream

import scala.xml.{ Elem, TopScope }
import scala.xml.parsing.NoBindingFactoryAdapter

import org.xml.sax.InputSource

import javax.xml.parsers.{ SAXParser, SAXParserFactory }
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{ Schema, SchemaFactory }
import logging.Logger

/**
 *
 */
object Validator

  extends Logger {

  def validateXSD(xmluri: String, xsduri: String): Boolean = {
    try {
      val schema = factory.newSchema(new StreamSource(xsduri))
      val validator = schema.newValidator
      validator.validate(new StreamSource(xmluri))
      true
    } catch {
      case e: Throwable ⇒
        error("XML validation error : " + e)
        false
    }
  }

  def validateXSD(xml: InputStream, xsd: InputStream): Boolean = {
    try {
      val schema = factory.newSchema(new StreamSource(xsd))
      val validator = schema.newValidator
      validator.validate(new StreamSource(xml))
      true
    } catch {
      case e: Throwable ⇒
        error("XML validation error : " + e)
        false
    }
  }

  class SchemaAwareFactoryAdapter(schema: Schema) extends NoBindingFactoryAdapter {

    def loadXML(source: InputSource, schema: Schema): Elem = {
      val parser: SAXParser = try {
        val f = SAXParserFactory.newInstance
        f.setNamespaceAware(true)
        f.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
        f.newSAXParser
      } catch {
        case e: Exception ⇒
          Console.err.println("error: Unable to instantiate parser")
          throw e
      }
      val vh = schema.newValidatorHandler
      vh.setContentHandler(this)
      val xr = parser.getXMLReader
      xr.setContentHandler(vh)
      scopeStack.push(TopScope)
      xr.parse(source)
      scopeStack.pop
      return rootElem.asInstanceOf[Elem]
    }
  }

  private[this] final val schemaLang = "http://www.w3.org/2001/XMLSchema"

  private[this] val factory = SchemaFactory.newInstance(schemaLang)

}