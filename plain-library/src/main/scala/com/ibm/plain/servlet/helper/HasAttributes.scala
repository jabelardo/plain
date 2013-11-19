package com.ibm

package plain

package servlet

package helper

import java.util.Enumeration

import scala.collection.JavaConversions.asJavaEnumeration
import scala.collection.concurrent.TrieMap

trait HasAttributes {

  def log(msg: String)

  final def getAttribute(name: String): Object = attributes.get(name) match {
    case Some(attr) ⇒ attr
    case _ ⇒ null
  }

  final def getAttributeNames: Enumeration[String] = attributes.keysIterator

  final def removeAttribute(name: String): Unit = attributes.remove(name)

  final def setAttribute(name: String, value: Object): Unit = attributes.put(name, value)

  private[this] final val attributes = new TrieMap[String, Object]

}
