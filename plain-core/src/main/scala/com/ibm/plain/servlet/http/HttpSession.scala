package com.ibm

package plain

package servlet

package http

import scala.collection.JavaConversions.enumerationAsScalaIterator

import javax.{ servlet ⇒ js }

import collection.mutable.LeastRecentlyUsedCache

final class HttpSession private (

  private[this] final val id: String,

  private[this] final val servletcontext: js.ServletContext)

    extends js.http.HttpSession

    with HasAttributes {

  final val getCreationTime: Long = time.now

  final def getId = id

  final def getLastAccessedTime: Long = lastaccesstime

  final def getMaxInactiveInterval: Int = maxinactiveinterval

  final val getServletContext: js.ServletContext = servletcontext

  @deprecated("", "") final def getSessionContext: js.http.HttpSessionContext = deprecated

  final def getValue(name: String): Object = getAttribute(name)

  final def getValueNames: Array[String] = getAttributeNames.toArray

  final def invalidate = HttpSession.destroy(id)

  final def isNew: Boolean = isnew

  final def putValue(name: String, value: Object) = setAttribute(name, value)

  final def removeValue(name: String) = removeAttribute(name)

  final def setMaxInactiveInterval(interval: Int) = maxinactiveinterval = interval

  final def setLastAccessedTime(value: Long) = lastaccesstime = value

  @inline private def fromCache: HttpSession = { isnew = false; this }

  private[this] final var maxinactiveinterval = -1

  private[this] final var lastaccesstime: Long = -1L

  private[this] final var isnew = true

}

private object HttpSession {

  final def create(id: String, servletcontext: js.ServletContext): HttpSession = {
    val session = new HttpSession(id, servletcontext)
    httpsessions.put(id, session)
    session
  }

  final def retrieve(id: String): HttpSession = httpsessions.get(id) match {
    case Some(session) ⇒ session.fromCache
    case _ ⇒ null
  }

  final def destroy(id: String): Unit = httpsessions.remove(id)

  private[this] final val httpsessions = LeastRecentlyUsedCache[HttpSession](maximumCachedSessions)

}
