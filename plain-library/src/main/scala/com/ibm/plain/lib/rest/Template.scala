package com.ibm.plain

package lib

package rest

import scala.annotation.tailrec
import scala.util.control.ControlThrowable

import http.Request
import http.Request._

/**
 * Error handling.
 */
case class InvalidTemplate(

  template: Template,

  reason: String)

  extends ControlThrowable {

  override final def toString = getClass.getName + "(template=" + template.path + ", reason=" + reason + ")"

}

/**
 * Variables are not allowed as start of a template path.
 */
abstract sealed class Element

final case class Segment(name: String, next: Element) extends Element

final case class Variable(name: String, next: Element) extends Element

final case class ResourceClass(resource: Class[Resource]) extends Element

/**
 *
 */
final case class Template(

  path: String,

  clazz: Class[Resource]) {

  final val root = if (0 == path.length) {
    ResourceClass(clazz)
  } else {
    path.split("/").reverse.foldLeft[Element](ResourceClass(clazz)) {
      case (elems, e) ⇒
        if (e.startsWith("{") && e.endsWith("}"))
          Variable(e.drop(1).dropRight(1), elems)
        else if (!(e.contains("{") || e.contains("}")))
          Segment(e, elems)
        else
          throw InvalidTemplate(this, "Neither a segment nor a variable.")
    }
  }

  override final def toString = root.toString

  require(!(path.startsWith("/") || path.endsWith("/")), "A path-template must not start or end with a '/' : " + path)

  require(!root.isInstanceOf[Variable], "A path-template must not start with a variable : " + path)

}

final case class Templates(

  resource: Option[Class[Resource]],

  branch: Option[Either[(String, Templates), Map[String, Templates]]]) {

  final def get(path: Path): Option[(Class[Resource], Variables, Path)] = {

    @inline @tailrec
    def get0(
      path: Path,
      variables: List[(String, String)],
      templates: Templates): Option[(Class[Resource], Variables, Path)] = {

      @inline def resource(tail: Path) = templates.resource match {
        case Some(resource) ⇒ Some((resource, variables.toMap, tail))
        case _ ⇒ None
      }

      path match {
        case Nil ⇒ resource(Nil)
        case l @ (head :: tail) ⇒ templates.branch match {
          case Some(Right(branch)) ⇒ branch.get(head) match {
            case Some(subbranch) ⇒ get0(tail, variables, subbranch)
            case _ ⇒ resource(l)
          }
          case Some(Left((name, branch))) ⇒ get0(tail, (name, head) :: variables, branch)
          case _ ⇒ resource(l)
        }
      }
    }

    get0(path, Nil, this)
  }

  override final def toString = {

    def inner(node: Templates, indent: String): String = {
      (node.resource match {
        case Some(resource) ⇒ indent + (if (node eq this) "/" else "") + " ⇒ " + resource.getName
        case _ ⇒ ""
      }) + (node.branch match {
        case Some(Left((name, node))) ⇒ inner(node, indent + "/{" + name + "}")
        case Some(Right(branch)) ⇒ branch.foldLeft("") { case (elem, e) ⇒ elem + inner(e._2, indent + "/" + e._1) }
        case _ ⇒ ""
      })
    }

    inner(this, "\n").split("\n").sorted.mkString("\n")
  }

}

object Templates {

  def apply(templates: Template*) = templates.foldLeft[Option[Templates]](None) {
    case (elems, e) ⇒ add(e, elems)
  }

  private[this] def add(template: Template, node: Option[Templates]): Option[Templates] = {

    def add0(element: Element, node: Option[Templates]): Templates = {

      @inline def alreadyVariable = throw InvalidTemplate(template, "Already a variable at this position.")

      @inline def alreadySegment = throw InvalidTemplate(template, "Already a segment at this position.")

      @inline def wrongVariable = throw InvalidTemplate(template, "Already a variable with a different name at this position.")

      element match {
        case Segment(name, next) ⇒ node match {
          case None ⇒ Templates(None, Some(Right(Map(name -> add0(next, None)))))
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Right(Map(name -> add0(next, None)))))
          case Some(Templates(resource, Some(Right(branch)))) ⇒ branch.get(name) match {
            case None ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add0(next, None)))))
            case s @ Some(_) ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add0(next, s)))))
          }
          case Some(Templates(resource, Some(Left((_, _))))) ⇒ alreadyVariable
        }
        case Variable(name, next) ⇒ node match {
          case None ⇒ Templates(None, Some(Left((name, add0(next, None)))))
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Left((name, add0(next, None)))))
          case Some(Templates(resource, Some(Left((oldname, branch))))) if oldname == name ⇒
            Templates(resource, Some(Left((name, add0(next, Some(branch))))))
          case Some(Templates(resource, Some(Left((oldname, branch))))) ⇒ wrongVariable
          case _ ⇒ alreadySegment
        }
        case ResourceClass(resource) ⇒ node match {
          case None ⇒ Templates(Some(resource), None)
          case Some(Templates(_, node)) ⇒ Templates(Some(resource), node)
        }
      }
    }

    Some(add0(template.root, node))
  }

}
