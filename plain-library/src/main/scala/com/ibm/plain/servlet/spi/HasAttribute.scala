package com.ibm

package plain

package servlet

package spi

import java.util.{ Collections, Enumeration, Map ⇒ JMap, HashMap ⇒ JHashMap }

import scala.collection.JavaConversions.{ asJavaCollection, enumerationAsScalaIterator, mutableMapAsJavaMap }
import scala.collection.mutable.OpenHashMap

/**
 *
 */
trait HasAttribute {

  final def getAttribute(name: String): Object = m.get(name)

  final def getAttributeNames: Enumeration[String] = m.getKeys

  final def setAttribute(name: String, value: Object): Unit = m.put(name, value)

  final def removeAttribute(name: String): Unit = m.remove(name)

  private[this] final lazy val m = Mapping.apply

}

trait HasValue {

  self: HasAttribute ⇒

  final def getValue(name: String): Object = getAttribute(name)

  final def getValueNames: Array[String] = getAttributeNames.toArray

  final def putValue(name: String, value: Object) = setAttribute(name, value)

  final def removeValue(name: String) = removeAttribute(name)

}

trait HasInitParameter {

  final def getInitParameter(name: String): String = m.getString(name)

  final def getInitParameterNames: Enumeration[String] = m.getKeys

  final def setInitParameter(name: String, value: String): Unit = m.put(name, value)

  private[this] final lazy val m = Mapping.apply

}

trait HasParameter {

  final def getParameter(name: String): String = m.getString(name)

  final def getParameterNames: Enumeration[String] = m.getKeys

  final def getParameterValues(name: String): Array[String] = Array(m.getString(name))

  final def getParameterMap: JMap[String, String] = m.asStringMap

  final def setParameter(name: String, value: String): Unit = m.put(name, value)

  private[this] final lazy val m = Mapping.apply

}

/**
 * Common implementation.
 */
final class Mapping(val map: OpenHashMap[String, Object])

  extends AnyVal {

  final def get(name: String): Object = { print("get: " + name); map.get(name) match { case Some(value) ⇒ println("=" + value); value case _ ⇒ println("=null"); null } }

  final def getString(name: String): String = { print("getString: " + name); map.get(name) match { case Some(value) ⇒ println("=" + value); value.asInstanceOf[String] case _ ⇒ println("=null"); null } }

  final def put(name: String, value: Object): Unit = { println("put: " + name + "=" + value); map.put(name, value) }

  final def remove(name: String): Unit = { println("remove: " + name); map.remove(name) }

  final def getKeys: Enumeration[String] = { println("keys " + map.keys); Collections.enumeration[String](map.keys) }

  final def getValues: Array[String] = map.map(_._2.toString).toArray

  final def asStringMap: JMap[String, String] = { val m = map.map(e ⇒ (e._1, e._2.toString)); println("asMap " + m); m }

}

object Mapping {

  def apply = new Mapping(new OpenHashMap[String, Object])

}

