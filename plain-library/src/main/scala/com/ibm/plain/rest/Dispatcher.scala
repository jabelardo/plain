package com.ibm

package plain

package rest

import aio.{ ControlCompleted, Io, transfer }
import aio.Iteratees.drop
import aio.FileByteChannel.forWriting
import concurrent.spawn
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
    import context.io
    templates match {
      case Some(root) ⇒ root.get(path) match {
        case Some((clazz, variables, remainder)) ⇒
          clazz.newInstance match {
            case resource: Resource ⇒
              entity match {
                case Some(ContentEntity(length, _)) if !method.entityallowed ⇒
                  spawn { transfer(context.io, forWriting(if (os.isWindows) "nul" else "/dev/null"), null); () }
                  request ++ None
                case Some(ContentEntity(length, _)) if method.entityallowed ⇒ io ++ length
                case Some(_) if !method.entityallowed ⇒ throw ServerError.`501`
                case _ ⇒
              }
              resource.handle(request, Context(resource) ++ io ++ variables ++ remainder ++ this)
            case _ ⇒ throw ServerError.`500`
          }
        case _ ⇒ throw ClientError.`404`
      }
      case _ ⇒ throw ServerError.`501`
    }
  }

  override final def completed(response: Response, context: Context): Nothing = { completed(response, context.io); throw ControlCompleted }

  override final def failed(e: Throwable, context: Context): Nothing = { failed(e, context.io); reflect.tryLocation; throw ControlCompleted }

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher

  extends Dispatcher(Templates(
    //    Template("user/{user}", Class.forName("com.ibm.plain.rest.resource.TestResource")),
    Template("ping", Class.forName("com.ibm.plain.rest.resource.PingResource")),
    //    Template("static", Class.forName("com.ibm.plain.rest.resource.DirectoryResource")),
    Template("echo", Class.forName("com.ibm.plain.rest.resource.EchoResource")))) {

  sys.runtime.gc

}
