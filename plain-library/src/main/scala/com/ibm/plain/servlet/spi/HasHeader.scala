package com.ibm

package plain

package servlet

package spi

import java.util.{ Collections, Enumeration }

import scala.collection.JavaConversions.{ asJavaCollection, seqAsJavaList }

trait HasHeader {

  self: HasContext ⇒

  final def getHeader(name: String): String = context.request.headers.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  final def getHeaderNames: Enumeration[String] = Collections.enumeration[String](context.request.headers.keys)

  final def getHeaders(name: String): Enumeration[String] = context.request.headers.get(name) match { case Some(value) ⇒ Collections.enumeration[String](List(value)) case _ ⇒ null }

  final def getIntHeader(name: String): Int = unsupported

  final def getDateHeader(name: String): Long = unsupported

  final def setHeader(name: String, value: String): Unit = context.response.headers = context.response.headers ++ Map(name -> value)

  final def setIntHeader(name: String, value: Int): Unit = unsupported

  final def setDateHeader(name: String, value: Long): Unit = context.response.headers = context.response.headers ++ Map(name -> new java.util.Date(value).toString)

  final def addHeader(name: String, value: String): Unit = unsupported

  final def addIntHeader(name: String, value: Int): Unit = unsupported

  final def addDateHeader(name: String, value: Long): Unit = unsupported

  final def containsHeader(name: String): Boolean = context.request.headers.contains(name)

}

