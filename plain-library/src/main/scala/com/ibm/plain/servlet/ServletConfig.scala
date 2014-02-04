package com.ibm

package plain

package servlet

import java.util.Enumeration
import javax.{ servlet ⇒ js }

import scala.xml.Node
import scala.language.postfixOps
import scala.collection.JavaConversions._

trait ServletConfig

  extends js.ServletConfig

  with aspect.MethodTracer { // :REMOVE:

  abstract override final def getServletName = name

  abstract override final def getInitParameter(name: String): String = initparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  abstract override final def getInitParameterNames: Enumeration[String] = initparameters.keysIterator

  abstract override final def getServletContext: js.ServletContext = servletcontext

  protected[this] val servletcontext: ServletContext

  protected[this] val servletxml: Node

  protected[this] final val initparameters = (servletxml \ "init-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap

  protected[this] final val name = (servletxml \ "servlet-name").text

}

final class SimpleServletConfig(

  protected[this] final val servletxml: Node,

  protected[this] final val servletcontext: ServletContext)

  extends js.ServletConfig {

  final def getServletName = name

  final def getInitParameter(name: String): String = initparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  final def getInitParameterNames: Enumeration[String] = initparameters.keysIterator

  final def getServletContext: js.ServletContext = servletcontext

  protected[this] final val initparameters = (servletxml \ "init-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap

  protected[this] final val name = (servletxml \ "servlet-name").text

}

