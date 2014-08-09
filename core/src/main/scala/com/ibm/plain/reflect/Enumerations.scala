package com.ibm

package plain

package reflect

import scala.language.existentials

import logging.Logger

/**
 * Use these enumerations instead of scala enumerations.
 *
 * @author michael.wellner@de.ibm.com
 * @since 3/18/14
 */
trait EnumerationCapabilities

    extends Logger {

  type EnumerationType

  val asString: EnumerationType ⇒ String

  /**
   * A Sequence of all enumeration values.
   */
  val values: Seq[EnumerationType]

  /**
   * Returns the enumeration value which is associated with the string.
   *
   * @param value
   * The value which should be transformed to the enumeration type.
   * @return
   * The related enumeration value.
   * @throws EnumerationValueNotFoundException
   * If the value cannot be transformed to the enumeration type.
   */
  def forString(value: String): EnumerationType = {
    values.find(asString(_) == value) match {
      case Some(enumerationValue) ⇒ enumerationValue
      case _ ⇒
        val e = EnumerationValueNotFoundException(this.getClass, value)
        error(e.getMessage)
        throw e
    }
  }

  final def valueOf(name: String) = forString(name)

}

abstract class AbstractEnumeration[T](val asString: T ⇒ String)

    extends EnumerationCapabilities {

  type EnumerationType = T

}

/**
 * Helper for simplified usage of [[EnumerationCapabilities]].
 *
 * Extend your enumeration type base class with [[EnumerationWithName.BaseType]] and use
 * [[EnumerationWithName.EnumerationCapabilities]] for your enumeration object.
 */
object EnumerationWithName {

  trait BaseType {

    val name: String

  }

  abstract class EnumerationCapabilities[T <: BaseType]

    extends AbstractEnumeration[T](_.name)

}

/**
 * Helper for simplified usage of [[EnumerationCapabilities]]. Extends [[EnumerationWithName]] by using the class name as the value for name.
 *
 * Extend your enumeration type base class with [[EnumerationWithClassName.BaseType]] and use
 * [[EnumerationWithClassName.EnumerationCapabilities]] for your enumeration object.
 */
object EnumerationWithClassName {

  trait BaseType

      extends EnumerationWithName.BaseType {

    final val name = try getClass.getSimpleName.replace("$", "") catch { case _: Throwable ⇒ scalifiedName(getClass) }

  }

  abstract class EnumerationCapabilities[T <: BaseType]

    extends EnumerationWithName.EnumerationCapabilities[T]

}

/**
 * Helper for simplified usage of [[EnumerationCapabilities]].
 *
 * Extend your enumeration type base class with [[EnumerationWithValue.BaseType]] and use
 * [[EnumerationWithValue.EnumerationCapabilities]] for your enumeration object.
 */
object EnumerationWithValue {

  trait BaseType {

    val value: String

  }

  abstract class EnumerationCapabilities[T <: BaseType]

    extends AbstractEnumeration[T](_.value)

}

/**
 * Exception which is thrown when a string value can't be transformed to an enumeration value.
 *
 * @param enumType
 * The type of the enumeration.
 * @param value
 * The value which should be transformed to the enumeration type.
 */
case class EnumerationValueNotFoundException(enumType: Class[_], value: String)

  extends Exception(s"The enumeration ${enumType.getSimpleName} does not have the value $value.")
