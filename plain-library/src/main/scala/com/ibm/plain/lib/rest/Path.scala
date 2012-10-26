package com.ibm.plain

package lib

package rest

import scala.util.control.ControlThrowable

/**
 * Error handling.
 */
case class InvalidTemplate(

  template: Template,

  reason: String)

  extends ControlThrowable {

  override final def toString = getClass.getName + "(template=" + template.template + ", reason=" + reason + ")"

}

/**
 * A path-template is build from segments and variables: template ::= segment ( segment | variable ) * null
 */
abstract sealed class Element(next: Element)

final case class Segment(name: String, next: Element) extends Element(next)

final case class Variable(name: String, next: Element) extends Element(next)

case object Null extends Element(null)

/**
 * for instance, "system/division/{division}/department/{department}"
 * for instance, "system/location/{location}"
 */
case class Template(template: String) {

  val root = template.split("/").reverse.foldLeft[Element](Null) {
    case (elems, e) ⇒
      if (e.startsWith("{") && e.endsWith("}"))
        Variable(e, elems)
      else
        Segment(e, elems)
  }.asInstanceOf[Segment]

  override final def toString = root.toString

  require(!(template.startsWith("/") || template.endsWith("/")), "A path-template must not start or end with a '/' : " + template)

}

import Templates._

final class Templates private (

  template: Template,

  prev: Map[String, Next]) {

  val roots = if (null == template) prev else prev.get(template.root.name) match {
    case None ⇒ template.root.next match {
      case variable: Variable ⇒ prev ++ Map(template.root.name -> Left(variable))
      case segment: Segment ⇒ prev ++ Map(template.root.name -> Right(Map(segment.name -> segment.next)))
      case _ ⇒ invalid(template, "Found a null-path-element.")
    }
    case Some(Right(next)) ⇒ template.root.next match {
      case segment: Segment ⇒ prev ++ Map(template.root.name -> Right(next ++ Map(segment.name -> segment.next)))
      case _ ⇒ invalid(template, "You must not mix path-segments and path-variables.")
    }
    case _ ⇒ invalid(template, "Only one path-variable is allowed at this path position.")
  }

  override final def toString = roots.toString

  @inline private[this] def invalid(template: Template, reason: String) = throw InvalidTemplate(template, reason)

}

object Templates {

  type Next = Either[Variable, Map[String, Element]]

  def apply(template: Template) = new Templates(template, Map.empty)

  def apply(templates: Template*): Templates = templates.foldLeft[Templates](empty) {
    case (elems, e) ⇒ new Templates(e, elems.roots)
  }

  val empty = apply(null)

}

