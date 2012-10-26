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

case class Templates(resource: Option[ResourceClass], branch: Option[Either[(String, Templates), Map[String, Templates]]]) {

  override final def toString = {

    def inner(node: Templates, indent: String): String = {
      (node.resource match {
        case Some(ResourceClass(resource)) ⇒ indent + " ⇒ " + resource.getName
        case _ ⇒ ""
      }) + (node.branch match {
        case Some(Left((name, node))) ⇒ inner(node, indent + "/" + name)
        case Some(Right(branch)) ⇒ branch.foldLeft("") { case (elem, e) ⇒ elem + inner(e._2, indent + "/" + e._1) }
        case None ⇒ ""
        case _ ⇒ "unhandled"
      })
    }

    "Templates : " + inner(this, "\n").split("\n").sorted.mkString("\n")
  }

}

object Templates {

  def apply(templates: Template*): Templates = templates.foldLeft[Option[Templates]](None) {
    case (elems, e) ⇒ apply(e, elems)
  }.get

  def apply(template: Template, node: Option[Templates]): Option[Templates] = {
    def add(element: Element, node: Option[Templates]): Templates = element match {
      case s @ Segment(name, next) ⇒
        node match {
          case None ⇒ Templates(None, Some(Right(Map(name -> add(next, None)))))
          case Some(Templates(resource, Some(Right(branch)))) ⇒ branch.get(name) match {
            case None ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add(next, None)))))
            case v @ Some(_) ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add(next, v)))))
          }
          case Some(Templates(resource, Some(Left((_, _))))) ⇒ invalid(template, "Already a variable here.")
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Right(Map(name -> add(next, None)))))
          case _ ⇒ invalid(template, "Not yet handled.")
        }
      case v @ Variable(name, next) ⇒
        node match {
          case None ⇒ Templates(None, Some(Left((v.name, add(next, None)))))
          case Some(Templates(resource, Some(Left((oldname, branch))))) if oldname == v.name ⇒
            Templates(resource, Some(Left((v.name, add(next, Some(branch))))))
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Left((v.name, add(next, None)))))
          case Some(Templates(resource, Some(Right(_)))) ⇒ invalid(template, "Already a segment here.")
          case _ ⇒ invalid(template, "Not yet handled.")
        }
      case r @ ResourceClass(resource) ⇒
        node match {
          case None ⇒ Templates(Some(r), None)
          case Some(Templates(None, node)) ⇒ Templates(Some(r), node)
          case _ ⇒ invalid(template, "Not yet handled.")
        }
    }

    Some(add(template.root, node))
  }

  @inline final def invalid(template: Template, reason: String) = throw InvalidTemplate(template.template, reason)

  @inline final def invalid(template: String, reason: String) = throw InvalidTemplate(template, reason)

}

