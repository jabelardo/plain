package com.ibm

package plain

package servlet

import java.util.Enumeration
import javax.{ servlet ⇒ js }

import scala.xml.Node
import scala.language.postfixOps
import scala.collection.JavaConversions._

/**
 *
 */
abstract class FilterConfig

    extends js.FilterConfig {

  override final def getFilterName = name

  override final def getInitParameter(name: String): String = initparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  override final def getInitParameterNames: Enumeration[String] = initparameters.keysIterator

  override final def getServletContext: js.ServletContext = servletcontext

  protected[this] val servletcontext: ServletContext

  protected[this] val filterxml: Node

  protected[this] final val initparameters = (filterxml \ "init-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap

  protected[this] final val name = (filterxml \ "filter-name").text

}

/**
 *
 */
final class WebXmlFilterConfig(

  protected[this] final val filterxml: Node,

  protected[this] final val servletcontext: ServletContext)

    extends FilterConfig
