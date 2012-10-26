package com.ibm.plain

package lib

package rest

import scala.util.control.ControlThrowable

/**
 * Error handling.
 */
case class InvalidTemplate(

  template: String,

  reason: String)

  extends ControlThrowable {

  override final def toString = getClass.getName + "(template=" + template + ", reason=" + reason + ")"

}

/**
 * A path-template is build from segments and variables: template ::= segment ( segment | variable ) * null
 */
abstract sealed class Element

final case class Segment(name: String, next: Element) extends Element

final case class Variable(name: String, next: Element) extends Element

final case class ResourceClass(resource: Class[Resource]) extends Element

/**
 * for instance, "system/division/{division}/department/{department}"
 * for instance, "system/location/{location}"
 */
case class Template(

  template: String,

  clazz: Class[Resource]) {

  val root = template.split("/").reverse.foldLeft[Element](ResourceClass(clazz)) {
    case (elems, e) ⇒
      if (e.startsWith("{") && e.endsWith("}"))
        Variable(e, elems)
      else if (!(e.contains("{") || e.contains("}")))
        Segment(e, elems)
      else
        Templates.invalid(template, "Neither a segment nor a variable.")
  }.asInstanceOf[Segment]

  override final def toString = root.toString

  require(!(template.startsWith("/") || template.endsWith("/")), "A path-template must not start or end with a '/' : " + template)

}

import Templates._

final class Templates private (

  template: Template,

  t: Map[String, Next]) {

  val templates = if (null == template) t else t.get(template.root.name) match {
    case None ⇒ template.root.next match {
      case variable: Variable ⇒ t ++ Map(template.root.name -> Left(variable))
      case segment: Segment ⇒ t ++ Map(template.root.name -> Right(Map(segment.name -> segment.next)))
      case _ ⇒ invalid(template, "Found a null-path-element.")
    }
    case Some(Right(next)) ⇒ template.root.next match {
      case segment: Segment ⇒ t ++ Map(template.root.name -> Right(next ++ Map(segment.name -> segment.next)))
      case _ ⇒ invalid(template, "You must not mix path-segments and path-variables.")
    }
    case _ ⇒ invalid(template, "Only one path-variable is allowed at this path position.")
  }

  override final def toString = templates.toString

}

object Templates {

  type Next = Either[Variable, Map[String, Element]]

  def apply(templates: Template*): Templates = templates.foldLeft[Templates](new Templates(null, Map.empty)) {
    case (elems, e) ⇒ new Templates(e, elems.templates)
  }

  @inline final def invalid(template: Template, reason: String) = throw InvalidTemplate(template.template, reason)

  @inline final def invalid(template: String, reason: String) = throw InvalidTemplate(template, reason)

}

