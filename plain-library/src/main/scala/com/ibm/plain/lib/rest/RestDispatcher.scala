package com.ibm.plain

package lib

package rest

import text.UTF8
import logging.HasLogger
import http._
import http.Entity._
import http.Status._

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class RestDispatcher

  extends HttpDispatcher

  with HasLogger {

  def dispatch(request: Request): Option[Response] = {

    register(List("ping"), Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]])

    println(request)

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

  }

  def register(path: Request.Path, clazz: Class[Resource]) = resources += ((path, clazz))

  private[this] final val resources = new scala.collection.mutable.HashMap[Request.Path, Class[Resource]]

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
