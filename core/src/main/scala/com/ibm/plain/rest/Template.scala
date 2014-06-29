package com.ibm

package plain

package rest

import scala.annotation.tailrec
import scala.language.existentials
import scala.util.control.ControlThrowable
import scala.collection.concurrent.TrieMap

import com.typesafe.config.Config

import config._
import http.Method
import http.Request.{ Path, Variables }
import resource.DirectoryResource

/**
 * Error handling.
 */
case class InvalidTemplateException(

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

final case class ResourceClass(resource: Class[_ <: Uniform], config: Config) extends Element

/**
 *
 */
final class Template private (

  val resource: Class[_ <: Uniform],

  val config: Config,

  val path: String) {

  final val root = if (0 == path.length) {
    ResourceClass(resource, config)
  } else {
    path.split("/").reverse.filter(_ != "*").foldLeft[Element](ResourceClass(resource, config)) {
      case (elems, e) ⇒
        if (e.startsWith("{") && e.endsWith("}"))
          Variable(e.drop(1).dropRight(1), elems)
        else if (!(e.contains("{") || e.contains("}")))
          Segment(e, elems)
        else
          throw InvalidTemplateException(this, "Neither a segment nor a variable.")
    }
  }

  override final def toString = root.toString

  require(!(path.startsWith("/") || path.endsWith("/")), "A path-template must not start or end with a '/' : " + path)

  require(!root.isInstanceOf[Variable], "A path-template must not start with a variable : " + path)

  if (classOf[Uniform].isAssignableFrom(resource)) require(null != resource.newInstance.asInstanceOf[Uniform], "Could not instantiate the given class. Did you misspell the absolute class name? (" + resource + ")")

}

object Template {

  def apply(path: String, clazz: Class[_], config: Config) = new Template(clazz.asInstanceOf[Class[_ <: Uniform]], config, path)

}

final case class Templates(

  resource: Option[(Class[_ <: Uniform], Config)],

  branch: Option[Either[(String, Templates), Map[String, Templates]]]) {

  final def get(method: Method, path: Path): Option[(Class[_ <: Uniform], Config, Option[Variables], Path)] = {

    @inline @tailrec
    def get0(
      path: Path,
      variables: Option[TrieMap[String, String]],
      templates: Templates): Option[(Class[_ <: Uniform], Config, Option[Variables], Path)] = {

      @inline def resource(tail: Path) = templates.resource match {
        case Some((resourceclass, config)) ⇒ Some((resourceclass, config, variables, tail))
        case _                             ⇒ None
      }

      path match {
        case Nil ⇒ resource(Nil)
        case p @ (head :: tail) ⇒
          templates.branch match {
            case Some(Right(branch)) ⇒ branch.get(head) match {
              case Some(subbranch) ⇒ get0(tail, variables, subbranch)
              case _               ⇒ resource(p)
            }
            case Some(Left((name, branch))) ⇒ get0(tail, { val v = variables match { case None ⇒ new TrieMap[String, String] case Some(v) ⇒ v }; v += ((name, head)); Some(v) }, branch)
            case _                          ⇒ resource(p)
          }
      }
    }

    get0(path, None, this)
  }

  override final def toString = {

    def inner(node: Templates, indent: String): String = {
      (node.resource match {
        case Some((resourceclass, config)) ⇒
          val c = config.toMap; indent + (if (node eq this) "/" else "") + " => " + resourceclass.getName + (if (!c.isEmpty) " => " + c else "")
        case _ ⇒ ""
      }) + (node.branch match {
        case Some(Left((name, node))) ⇒ inner(node, indent + "/{" + name + "}")
        case Some(Right(branch))      ⇒ branch.foldLeft("") { case (elem, e) ⇒ elem + inner(e._2, indent + "/" + e._1) }
        case _                        ⇒ ""
      })
    }

    inner(this, "\n").split("\n").sorted.mkString("\n")
  }

}

object Templates {

  def apply(templates: Seq[Template]) = templates.foldLeft[Option[Templates]](None) {
    case (elems, e) ⇒ add(e, elems)
  }

  private[this] def add(template: Template, node: Option[Templates]): Option[Templates] = {

    def add0(element: Element, node: Option[Templates]): Templates = {

      @inline def alreadyVariable = throw InvalidTemplateException(template, "Already a variable at this position.")

      @inline def alreadySegment = throw InvalidTemplateException(template, "Already a segment at this position.")

      @inline def wrongVariable = throw InvalidTemplateException(template, "Already a variable with a different name at this position.")

      element match {
        case Segment(name, next) ⇒ node match {
          case None                            ⇒ Templates(None, Some(Right(Map(name -> add0(next, None)))))
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Right(Map(name -> add0(next, None)))))
          case Some(Templates(resource, Some(Right(branch)))) ⇒ branch.get(name) match {
            case None        ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add0(next, None)))))
            case s @ Some(_) ⇒ Templates(resource, Some(Right(branch ++ Map(name -> add0(next, s)))))
          }
          case Some(Templates(resource, Some(Left((_, _))))) ⇒ alreadyVariable
        }
        case Variable(name, next) ⇒ node match {
          case None                            ⇒ Templates(None, Some(Left((name, add0(next, None)))))
          case Some(Templates(resource, None)) ⇒ Templates(resource, Some(Left((name, add0(next, None)))))
          case Some(Templates(resource, Some(Left((oldname, branch))))) if oldname == name ⇒
            Templates(resource, Some(Left((name, add0(next, Some(branch))))))
          case Some(Templates(resource, Some(Left((oldname, branch))))) ⇒ wrongVariable
          case _ ⇒ alreadySegment
        }
        case ResourceClass(resource, config) ⇒ node match {
          case None                     ⇒ Templates(Some((resource, config)), None)
          case Some(Templates(_, node)) ⇒ Templates(Some((resource, config)), node)
        }
      }
    }

    if (null != template) Some(add0(template.root, node)) else None
  }

}
