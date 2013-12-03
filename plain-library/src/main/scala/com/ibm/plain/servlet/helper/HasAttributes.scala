package com.ibm

package plain

package servlet

package helper

import java.util.Enumeration

import scala.collection.JavaConversions.asJavaEnumeration
import scala.collection.mutable.Map

trait HasAttributes {

  final def getAttribute(name: String): Object = attributes.get(name) match {
    case Some(attr) ⇒ attr
    case _ ⇒ null
  }

  final def getAttributeNames: Enumeration[String] = attributes.keysIterator

  final def removeAttribute(name: String): Unit = attributes.remove(name)

  final def setAttribute(name: String, value: Object): Unit = attributes.put(name, value)

  protected val attributes: Map[String, Object]

}
