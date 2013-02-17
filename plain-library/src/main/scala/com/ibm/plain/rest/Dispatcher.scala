package com.ibm

package plain

package rest

import aio.{ ControlCompleted, Io }
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

  final def dispatch(request: Request, io: Io): Nothing = handle(Context(io) ++ request)

  final def handle(context: Context): Nothing = {
    import context.io
    import context.request
    templates match {
      case Some(root) ⇒ root.get(request.path) match {
        case Some((clazz, variables, remainder)) ⇒
          clazz.newInstance match {
            case resource: Resource ⇒
              request.entity match {
                case Some(ContentEntity(length, _)) if !request.method.entityallowed ⇒
                  // spawn { transfer(context.io, forWriting(if (os.isWindows) "nul" else "/dev/null"), null); () }
                  request ++ None
                case Some(ContentEntity(_, length)) if request.method.entityallowed ⇒ io ++ length
                case Some(_) if !request.method.entityallowed ⇒ throw ServerError.`501`
                case _ ⇒
              }
              resource.handle(context ++ variables ++ remainder ++ this)
            case _ ⇒ throw ServerError.`500`
          }
        case _ ⇒ throw ClientError.`404`
      }
      case _ ⇒ throw ServerError.`501`
    }
  }

  override final def completed(context: Context): Nothing = { completed(context.response, context.io); throw ControlCompleted }

  override final def failed(e: Throwable, context: Context): Nothing = { failed(e, context.io); throw ControlCompleted }

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher

  extends Dispatcher(Templates(
    //    Template("user/{user}", Class.forName("com.ibm.plain.rest.resource.TestResource")),
    Template("ping", Class.forName("com.ibm.plain.rest.resource.PingResource")),
    Template("static", Class.forName("com.ibm.plain.rest.resource.DirectoryResource")),
    Template("echo", Class.forName("com.ibm.plain.rest.resource.EchoResource")))) {

  sys.runtime.gc

}
