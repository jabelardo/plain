package com.ibm.plain

package lib

import java.lang.reflect.{ Method, Modifier }

import scala.reflect.ClassTag

/**
 * Some tools to ease the use the Java reflection api in Scala.
 */
package object reflect {

  /**
   * Returns the primitive corresponding to it, for example Int for java.lang.Integer
   */
  def primitive(clazz: Class[_]) = clazz.getName match {
    case "java.lang.Boolean" ⇒ classOf[Boolean]
    case "java.lang.Byte" ⇒ classOf[Byte]
    case "java.lang.Character" ⇒ classOf[Char]
    case "java.lang.Short" ⇒ classOf[Short]
    case "java.lang.Integer" ⇒ classOf[Int]
    case "java.lang.Long" ⇒ classOf[Long]
    case "java.lang.Float" ⇒ classOf[Float]
    case "java.lang.Double" ⇒ classOf[Double]
    case _ ⇒ clazz
  }

  /**
   * Returns the boxed corresponding to it, for example java.lang.Integer for Int
   */
  def boxed(clazz: Class[_]) = clazz.getName match {
    case "boolean" ⇒ classOf[java.lang.Boolean]
    case "byte" ⇒ classOf[java.lang.Byte]
    case "char" ⇒ classOf[java.lang.Character]
    case "short" ⇒ classOf[java.lang.Short]
    case "int" ⇒ classOf[java.lang.Integer]
    case "long" ⇒ classOf[java.lang.Long]
    case "float" ⇒ classOf[java.lang.Float]
    case "double" ⇒ classOf[java.lang.Double]
    case _ ⇒ clazz
  }

  /**
   * Returns the boxed value for the given primitive value: simply call p.asInstanceOf[AnyRef]
   */

  /**
   * Returns a 'scala-safe' getSimpleName for the provided object's class.
   */
  def simpleName(a: AnyRef): String = simpleName(a.getClass)

  /**
   * Returns a 'scala-safe' getSimpleName for the provided class.
   */
  def simpleName(cls: Class[_]): String = {
    var n = cls.getName
    if (n.endsWith("$")) n = n.take(n.length - 1)
    val dollar = n.lastIndexOf('$')
    val dot = n.lastIndexOf('.')
    n.substring(scala.math.max(dollar, dot) + 1)
  }

  /**
   * Returns a 'scala-safe' getSimpleName for the provided class' parent.
   */
  def simpleParentName(cls: Class[_]): String = {
    val n = cls.getName
    val last = n.lastIndexOf('$')
    if (-1 < last) {
      val previous = n.take(last - 1).lastIndexOf('$')
      n.substring(previous + 1, last)
    } else n
  }

  /**
   * Returns the class name stripped of any '$', this way classes and companion objects can be set equal.
   */
  def strippedName(cls: Class[_]): String = cls.getName.replace("$", "")

}
