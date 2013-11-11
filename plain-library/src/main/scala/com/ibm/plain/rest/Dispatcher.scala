package com.ibm

package plain

package rest

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.concurrent.TrieMap

import aio.Io
import aio.Iteratees.drop
import aio.FileByteChannel.forWriting
import config._
import http.{ Request, Response }
import http.{ Dispatcher ⇒ HttpDispatcher }
import http.Entity.ContentEntity
import http.Status.{ ClientError, ServerError }

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class Dispatcher

  extends HttpDispatcher {

  @inline final def dispatch(request: Request, io: Io) = handle(Context(io) ++ request)

  final def handle(context: Context) = {
    import context.io
    import context.request
    resources.get(request.path.mkString("/")) match {
      case Some(resource) ⇒ resource.handle(context)
      case _ ⇒ templates match {
        case Some(root) ⇒ root.get(request.path) match {
          case Some((clazz, config, variables, remainder)) ⇒
            clazz.newInstance match {
              case resource: BaseResource ⇒
                request.entity match {
                  case Some(ContentEntity(_, length)) if request.method.entityallowed ⇒
                  case Some(_) if !request.method.entityallowed ⇒ throw ServerError.`501`
                  case _ ⇒
                }
                if (config.isEmpty && variables.isEmpty && remainder.isEmpty) resources.put(request.path.mkString("/"), resource)
                resource.handle(context ++ config ++ variables ++ remainder)
              case _ ⇒ throw ServerError.`500`
            }
          case _ ⇒ throw ClientError.`404`
        }
        case _ ⇒ throw ServerError.`501`
      }
    }
  }

  final def init = {
    templates = Templates(config.getConfigList("routes", List.empty).map { c: Config ⇒
      Template(c.getString("uri"), Class.forName(c.getString("resource-class-name")), c.getConfig("resource-config", ConfigFactory.empty))
    })
    if (log.isDebugEnabled) debug(getClass.getSimpleName + "(name=" + name + ", routes=" + templates.get + ")")
  }

  protected[this] final var templates: Option[Templates] = None

  private[this] final val resources = new TrieMap[String, BaseResource]

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultDispatcher extends Dispatcher
