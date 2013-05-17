package com.ibm

package plain

package servlet

package spi

trait HasPath {

  self: HasContext ⇒

  final def getMethod: String = context.request.method.toString

  final def getPathInfo: String = if (null != context.remainder) context.remainder.mkString("/", "/", "") else null

  final def getPathTranslated: String = unsupported

  final def getServletPath: String = getRequestURI

  final def getQueryString: String = { val q = context.request.query match { case Some(value) ⇒ value case _ ⇒ null }; println("q " + q); q }

  final def getRequestURI: String = context.request.path.mkString("/", "/", "")

  final def getRequestURL: StringBuffer = new StringBuffer(getRequestURI)

}

