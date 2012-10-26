package com.ibm.plain

package lib

package rest

import text.UTF8
import logging.HasLogger
import http._
import http.Entity._
import http.Status._
import scala.collection.immutable.HashMap

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class RestDispatcher

  extends HttpDispatcher

  with HasLogger {

  def dispatch(request: Request): Option[Response] = {

    // find the REST services classes now and dispatch to the right instance to the right method with all path variables set.

    resources.get(request.path) match {
      case Some(resourceclass) ⇒
        resourceclass.newInstance match {
          case resource: BaseResource ⇒
            resource.request = request
            Some(Response(resource.get))
          case c ⇒
            error("Class must inherit from BaseRequest : " + c)
            throw ServerError.`501`
        }
      case None ⇒ throw ClientError.`404`
    }

    None

  }

  /**
   * Michaels stuff - tbc tomorrow ...
   *
   * TODO: url -> variable
   * TODO: container around resource
   * TODO: ...do some performance checks on get method...
   * TODO: ...and maybe i should test it...
   */

  class StaticPathSegment(value: String)

  object StaticPathSegment {

    def apply(value: String): Option[StaticPathSegment] = value.charAt(0) match {
      case '{' ⇒ None // path segment is variable definition
      case _ ⇒ Some(new StaticPathSegment(value))
    }

  }

  case class Node(branch: HashMap[Option[StaticPathSegment], Node], clazz: Option[Class[Resource]]) {

    def get(path: Request.Path): Option[Class[Resource]] = path match {
      case Nil ⇒
        clazz
      case head :: tail ⇒
        branch.get(StaticPathSegment(head)) match {
          case Some(node) ⇒ node.get(tail)
          case None ⇒ None
        }
    }

  }

  def register(path: Request.Path, clazz: Class[Resource]) {
    def register(path: Request.Path, clazz: Class[Resource], node: Option[Node]): Node = {
      path match {
        case Nil ⇒ // no path element available -> Attach Resource to tree
          node match {
            case None ⇒ Node(HashMap(), Some(clazz))
            case Some(Node(map, None)) ⇒ Node(map, Some(clazz))
            case Some(Node(_, Some(_))) ⇒ throw new Exception("Path already exists.")
          }
        case head :: tail ⇒
          node match {
            case None ⇒
              Node(HashMap(StaticPathSegment(head) -> register(tail, clazz, None)), None)
            case Some(Node(map, c)) if map.contains(StaticPathSegment(head)) ⇒
              Node(map.map(t ⇒ if (t._1.eq(StaticPathSegment(head))) t._1 -> register(tail, clazz, Some(t._2)) else t), c)
            case Some(Node(map, c)) ⇒
              Node(map ++ HashMap(StaticPathSegment(head) -> register(tail, clazz, None)), c)
          }
      }
    }

    this.resources = register(path, clazz, Some(this.resources));
  }

  /*
   * I don't like variables - Maybe we should initialize resources during initialization
   * -> Having a list of Resources as constructor parameter ... 
   */
  private[this] final var resources = Node(HashMap(), None)

  register(List("ping"), Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]])

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultRestDispatcher

  extends RestDispatcher {

  override def dispatch(request: Request): Option[Response] = super.dispatch(request) match {
    case None ⇒ Some(Response(Status.ServerError.`501`))
    case e ⇒ e
  }

}
