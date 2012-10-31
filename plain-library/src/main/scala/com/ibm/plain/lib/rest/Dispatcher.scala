package com.ibm.plain

package lib

package rest

import com.ibm.plain.lib.aio.ReadChannel.asynchronousByteChannel2ReadChannel
import com.ibm.plain.lib.http.Response

import aio.Io
import http.{ Dispatcher ⇒ HttpDispatcher, Request }
import http.Entity.{ ContentEntity, RequestEntity }
import http.Status.{ ClientError, ServerError }

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class Dispatcher(templates: Option[Templates])

  extends HttpDispatcher

  with BaseUniform {

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

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher

  extends Dispatcher(Templates(
    Template("user/{user}", Class.forName("com.ibm.plain.lib.rest.resource.PingResource").asInstanceOf[Class[Resource]]),
    Template("echo", Class.forName("com.ibm.plain.lib.rest.resource.EchoResource").asInstanceOf[Class[Resource]])))  

