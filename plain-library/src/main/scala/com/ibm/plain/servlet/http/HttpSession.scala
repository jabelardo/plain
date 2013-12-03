package com.ibm

package plain

package servlet

package http

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.concurrent.TrieMap

import com.ibm.plain.servlet.ServletContext

import javax.{ servlet ⇒ js }

final class HttpSession(

  private[this] final val id: String,

  private[this] final val servletcontext: ServletContext)

  extends js.http.HttpSession

  with helper.HasAttributes {

  final val getCreationTime: Long = time.now

  final def getId = id

  final def getLastAccessedTime: Long = lastaccesstime

  final def getMaxInactiveInterval: Int = maxinactiveinterval

  final def getServletContext: js.ServletContext = servletcontext

  @deprecated("", "") final def getSessionContext: js.http.HttpSessionContext = deprecated

  final def getValue(name: String): Object = getAttribute(name)

  final def getValueNames: Array[String] = getAttributeNames.toArray

  final def invalidate = unsupported

  final def isNew: Boolean = unsupported

  final def putValue(name: String, value: Object) = setAttribute(name, value)

  final def removeValue(name: String) = removeAttribute(name)

  final def setMaxInactiveInterval(interval: Int) = maxinactiveinterval = interval

  final def setLastAccessedTime(value: Long) = lastaccesstime = value

  protected[this] final val attributes = new TrieMap[String, Object]

  @volatile private[this] final var maxinactiveinterval = 60 * 60

  @volatile private[this] final var lastaccesstime: Long = time.now

}
