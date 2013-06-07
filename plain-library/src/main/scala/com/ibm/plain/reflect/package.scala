package com.ibm

package plain

import java.lang.reflect.{ Method, Modifier }
import scala.collection.JavaConversions._
import scala.reflect.runtime.universe._
import org.reflections.Reflections
import org.reflections.util._
import org.reflections.scanners._

/**
 * Some tools to ease the use the Java reflection api in Scala.
 */
package object reflect {

  final val mirror = runtimeMirror(getClass.getClassLoader)

  final def toTuple[A <: Object](as: Seq[A]): Product = {
    val tuple = Class.forName("scala.Tuple" + as.size)
    tuple.getConstructors.apply(0).newInstance(as: _*).asInstanceOf[Product]
  }

  final lazy val reflections = new Reflections("com.ibm.plain") // not working with OneJar, needs work-around

  def subClasses[A](clazz: Class[A]): Set[Class[_ <: A]] = reflections.getSubTypesOf(clazz).toSet

  def tryBoolean(p: ⇒ Any, show: Boolean = false) = try { p; true } catch { case e: Throwable ⇒ if (show) println(e); false }

  def tryLocation = try { throw new Exception("tryLocation thrown.") } catch { case e: Throwable ⇒ e.printStackTrace }

  def stackSize = try { throw new Exception("stackSize thrown.") } catch { case e: Throwable ⇒ println("stackSize : " + e.getStackTrace.size) }

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
   * Returns a 'scala-safe' getSimpleName for the provided class.
   */
  def simpleName(n: String): String = {
    val last = n.lastIndexOf('$')
    if (-1 < last) {
      val prev = n.lastIndexOf('$', last - 1)
      n.substring(prev + 1, last)
    } else n
  }

  /**
   * Returns a 'scala-safe' getSimpleName for the provided class' parent.
   */
  def simpleParentName(n: String): String = {
    val last = n.lastIndexOf('$', n.length - 2)
    if (-1 < last) {
      val prev = n.lastIndexOf('$', last - 1)
      n.substring(prev + 1, last)
    } else n
  }

  /**
   * Return a 'beautiful' name for a class/object that was named with ``, this is so! expensive please only call it only once e.g. on  objects.
   * Please note: Usually case class/object .toString does exactly this, alas sometimes it doesn't, e.g. if your hierarchy extends from a Function.
   */
  def scalifiedName(cls: Class[_]): String = simpleName(cls.getName
    .replace("$eq", "=")
    .replace("$u002E", ".")
    .replace("$greater", ">")
    .replace("$less", "<")
    .replace("$plus", "+")
    .replace("$minus", "-")
    .replace("$times", "*")
    .replace("$div", "/")
    .replace("$bang", "!")
    .replace("$at", "@")
    .replace("$hash", "#")
    .replace("$percent", "%")
    .replace("$up", "^")
    .replace("$amp", "&")
    .replace("$tilde", "~")
    .replace("$qmark", "?")
    .replace("$bar", "|")
    .replace("$bslash", "\\")
    .replace("$colon", ":"))

}
