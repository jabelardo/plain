package com.ibm

package plain

package servlet

import java.util.Enumeration
import javax.servlet.{ ServletConfig ⇒ JServletConfig, ServletContext ⇒ JServletContext }

import scala.xml.Node
import scala.language.postfixOps
import scala.collection.JavaConversions._

trait ServletConfig

  extends JServletConfig {

  abstract override final def getServletName = name

  abstract override final def getInitParameter(name: String): String = initparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  abstract override final def getInitParameterNames: Enumeration[String] = initparameters.keysIterator

  abstract override final def getServletContext: JServletContext = servletcontext

  protected[this] val servletcontext: ServletContext

  protected[this] val servletxml: Node

  protected[this] final val initparameters = (servletxml \ "init-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap

  protected[this] final val name = (servletxml \ "servlet-name").text

}