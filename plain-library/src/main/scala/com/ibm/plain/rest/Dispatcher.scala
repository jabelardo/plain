package com.ibm

package plain

package rest

import aio.Io
import http.{ Dispatcher ⇒ HttpDispatcher, Request, Response }
import http.Status.{ ClientError, ServerError }

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */

trait TemplatesDispatcher

  extends HttpDispatcher

  with BaseUniform {

  val templates: Option[Templates]

  def dispatch(request: Request, io: Io): Nothing = handle(request, Context(io))

  def handle(request: Request, context: Context): Nothing = {
    templates match {
      case Some(root) ⇒ root.get(request.path) match {
        case Some((clazz, variables, remainder)) ⇒
          clazz.newInstance match {
            case resource: Resource ⇒ resource.handle(request, context ++ variables ++ remainder ++ this)
            case _ ⇒ throw ServerError.`500`
          }
        case None ⇒ throw ClientError.`404`
      }
      case None ⇒ throw ServerError.`501`
    }
  }

  override def completed(response: Response, context: Context) = completed(response, context.io)

  override def failed(e: Throwable, context: Context) = failed(e, context.io)
}

abstract class Dispatcher(val templates: Option[Templates]) extends TemplatesDispatcher

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher

  extends Dispatcher(Templates(
    Template("user/{user}", Class.forName("com.ibm.plain.rest.resource.PingResource")),
    Template("static", Class.forName("com.ibm.plain.rest.resource.DirectoryResource")),
    Template("echo", Class.forName("com.ibm.plain.rest.resource.EchoResource")))) {

}

class PlainDSLDispatcher(resources: List[Class[_ <: IsPlainDSLResource]])

  extends TemplatesDispatcher {

  val templates = Templates(resources.map(cl => Template(cl.newInstance().path, cl)))
  
}