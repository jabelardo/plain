package com.ibm

package plain

package rest

import aio.Io
import aio.Iteratees.drop
import http.{ Request, Response }
import http.{ Dispatcher ⇒ HttpDispatcher }
import http.Entity.ContentEntity
import http.Status.{ ClientError, ServerError }

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class Dispatcher(templates: Option[Templates])

  extends HttpDispatcher

  with BaseUniform {

  final def dispatch(request: Request, io: Io): Nothing = handle(request, Context(io))

  final def handle(request: Request, context: Context): Nothing = {
    import request._
    templates match {
      case Some(root) ⇒ root.get(request.path) match {
        case Some((clazz, variables, remainder)) ⇒
          clazz.newInstance match {
            case resource: Resource ⇒
              entity match {
                case Some(ContentEntity(length, _)) if !method.entityallowed && length < Int.MaxValue ⇒ drop(length.toInt)
                case Some(_) if !method.entityallowed ⇒ throw ServerError.`501`
                case _ ⇒
              }

              resource.handle(request, context ++ variables ++ remainder ++ this)

            case _ ⇒ throw ServerError.`500`
          }
        case None ⇒ throw ClientError.`404`
      }
      case None ⇒ throw ServerError.`501`
    }
  }

  override final def completed(response: Response, context: Context) = completed(response, context.io)

  override final def failed(e: Throwable, context: Context) = failed(e, context.io)

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher

  extends Dispatcher(Templates(
    Template("user/{user}", Class.forName("com.ibm.plain.rest.resource.PingResource")),
    Template("static", Class.forName("com.ibm.plain.rest.resource.DirectoryResource")),
    Template("echo", Class.forName("com.ibm.plain.rest.resource.EchoResource")))) {

}
