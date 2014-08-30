package com.ibm

package plain

import java.lang.management.ManagementFactory

import scala.collection.JavaConversions.asScalaSet
import scala.reflect.runtime.universe._
import scala.reflect.runtime.currentMirror

import org.reflections.Reflections

/**
 * Some tools to ease the use the Java reflection api in Scala.
 */
import java.lang.management.ManagementFactory

import scala.collection.JavaConversions.asScalaSet
import scala.reflect.runtime.universe.runtimeMirror

import org.reflections.Reflections

package object reflect

    extends config.CheckedConfig {

  import config._
  import settings._

  final val mirror = runtimeMirror(getClass.getClassLoader)

  final val prefixes = getStringList("plain.reflection.prefixes", List("com.ibm.plain"))

  final def toTuple[A <: Object](as: Seq[A]): Product = {
    val tuple = Class.forName("scala.Tuple" + as.size)
    tuple.getConstructors.apply(0).newInstance(as: _*).asInstanceOf[Product]
  }

  final lazy val reflections = {
    val logger = logging.createLogger(this)
    logger.info("Loading all class information via reflection : " + prefixes)
    val reflection = new Reflections(prefixes: _*)
    logger.debug("All class information loaded.")
    reflection
  }

  final def subClasses[A](clazz: Class[A]): Set[Class[_ <: A]] = reflections.getSubTypesOf(clazz).toSet

  final def stackSize = try { throw new Exception("stackSize thrown.") } catch { case e: Throwable ⇒ println("stackSize : " + e.getStackTrace.size) }

  /**
   * No error checking, existance is assumed. Use CompanionObject instead to get an optional companion.
   */
  final def companion(runtimeclass: Class[_]): Any = {
    val cs = currentMirror.classSymbol(runtimeclass)
    val cm = cs.companion.asModule
    val mm = mirror.reflectModule(cm)
    mm.instance
  }

  /**
   * Returns the primitive corresponding to it, for example Int for java.lang.Integer
   */
  final def primitive(clazz: Class[_]) = clazz.getName match {
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
  final def boxed(clazz: Class[_]) = clazz.getName match {
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
  final def simpleName(n: String): String = {
    val last = n.lastIndexOf('$')
    if (-1 < last) {
      val prev = n.lastIndexOf('$', last - 1)
      n.substring(prev + 1, last)
    } else n
  }

  /**
   * Returns a 'scala-safe' getSimpleName for the provided class' parent.
   */
  final def simpleParentName(n: String): String = {
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
  final def scalifiedName(cls: Class[_]): String = simpleName(cls.getName
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
