package com.ibm

package plain

package xml

import scala.annotation.meta.field
import scala.collection.JavaConversions._

import javax.xml.bind.annotation.{ XmlAnyElement, XmlAttribute, XmlElement, XmlElementRef, XmlElementRefs, XmlElementWrapper, XmlRootElement, XmlTransient }
import javax.xml.bind.annotation.adapters.{ XmlAdapter, XmlJavaTypeAdapter }

import java.util.{ Vector ⇒ JVector }
import java.math.{ BigDecimal ⇒ JBigDecimal }

object Adapter {

  class OptionAdapter[A](nones: A*) extends XmlAdapter[A, Option[A]] {
    def marshal(v: Option[A]): A = v.getOrElse(nones(0))
    def unmarshal(v: A) = if (nones contains v) None else Some(v)
  }

  final class StringOptionAdapter extends OptionAdapter[String]("", "null", "NULL", "Null")

  final class BooleanOptionAdapter extends OptionAdapter[Boolean](false)

  final class IntOptionAdapter extends OptionAdapter[Int](-1)

  final class LongOptionAdapter extends OptionAdapter[Long](-1L)

  final class BigDecimalAdapter extends XmlAdapter[JBigDecimal, BigDecimal] {
    import BigDecimal.javaBigDecimal2bigDecimal
    def unmarshal(v: JBigDecimal): BigDecimal = v
    def marshal(v: BigDecimal): JBigDecimal = v.underlying
  }

  final class CDataAdapter extends XmlAdapter[String, String] {
    def marshal(v: String): String = "<![CDATA[" + (if (v.contains("\n")) "\n" else "") + v + "]]>"
    def unmarshal(v: String) = text.fromBase64String(v, text.`UTF-8`)
  }

  final class ThrowableAdapter extends XmlAdapter[String, Throwable] {
    def marshal(e: Throwable): String = text.anyToBase64(e)
    def unmarshal(s: String): Throwable = text.anyFromBase64(s)
  }

  final case class ElementsWrapper[A](@xmlElementRef elements: JVector[A]) {
    def this() = this(null)
  }

  final class ElementsAdapter[A] extends XmlAdapter[ElementsWrapper[A], Seq[A]] {
    def marshal(v: Seq[A]) = ElementsWrapper[A](if (v == null) new JVector[A] else new JVector(v))
    def unmarshal(v: ElementsWrapper[A]) = v.elements.toSeq
  }

  @XmlRootElement(name = "list-element")
  final case class StringListElement(
    @xmlAttribute(required = true) element: String) {
    def this() = this(null)
  }

  final case class StringListWrapper(@xmlElementRef elements: java.util.Vector[StringListElement]) {
    def this() = this(null)
  }

  final class StringListAdapter extends XmlAdapter[StringListWrapper, Seq[String]] {
    def marshal(v: Seq[String]) = if (v == null)
      StringListWrapper(new java.util.Vector[StringListElement])
    else
      StringListWrapper(new java.util.Vector(v.map(StringListElement(_))))
    def unmarshal(v: StringListWrapper) = v.elements.map(_.element)
  }

  @XmlRootElement(name = "property")
  final case class Property(
    @xmlJavaTypeAdapter(classOf[CDataAdapter]) name: String,
    @xmlJavaTypeAdapter(classOf[CDataAdapter]) value: String) {
    def this() = this(null, null)
  }

  final case class PropertiesWrapper(@xmlElementRef properties: JVector[Property]) {
    def this() = this(null)
  }

  final class PropertiesAdapter extends XmlAdapter[PropertiesWrapper, Map[String, String]] {
    def marshal(v: Map[String, String]): PropertiesWrapper = if (v == null)
      PropertiesWrapper(new JVector[Property])
    else
      PropertiesWrapper(new JVector(v.map { case (n, v) ⇒ Property(n, v) }))
    def unmarshal(v: PropertiesWrapper): Map[String, String] = v.properties.foldLeft(Map[String, String]()) { case (m, e) ⇒ m ++ Map(e.name -> e.value) }
  }

}