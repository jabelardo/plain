package com.ibm.plain

package lib

package rest

import com.ibm.plain.lib.http.Response

import aio._
import http.{ HttpDispatcher, Request }
import http.Status.{ ClientError, ServerError }
import http.Entity._

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class RestDispatcher(templates: Option[Templates])

  extends HttpDispatcher {

  def dispatch(request: Request): Option[Response] = {
    println("dispatch " + request)
    templates match {
      case Some(root) ⇒ root.get(request.path) match {
        case Some((clazz, variables, remainder)) ⇒
          val resource = clazz.newInstance.asInstanceOf[BaseResource]
          resource.variables_ = variables
          resource.remainder_ = remainder
          resource.dispatcher = this
          resource.io = io
          resource.handle(request ++ Some(RequestEntity(io.channel)))
        case None ⇒ throw ClientError.`404`
      }
      case None ⇒ throw ServerError.`501`
    }
  }

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultRestDispatcher

  extends RestDispatcher(Templates(
    Template("user/{user}", Class.forName("com.ibm.plain.lib.rest.resource.PingResource").asInstanceOf[Class[Resource]]),
    Template("echo", Class.forName("com.ibm.plain.lib.rest.resource.EchoResource").asInstanceOf[Class[Resource]])))  

