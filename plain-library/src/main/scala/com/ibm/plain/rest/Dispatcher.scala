package com.ibm

package plain

package rest

import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import aio.Io
import aio.Iteratees.drop
import aio.FileByteChannel.forWriting
import config._
import http.{ Request, Response }
import http.{ Dispatcher ⇒ HttpDispatcher }
import http.Entity.ContentEntity
import http.Status.{ ClientError, ServerError }
import servlet.ServletContainer
import servlet.http.HttpServletResource
import org.apache.commons.io.FilenameUtils

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class Dispatcher

  extends HttpDispatcher {

  @inline final def dispatch(request: Request, io: Io) = handle(Context(io) ++ request)

  final def handle(context: Context) = {
    import context.request
    templates.get(request.method, request.path) match {
      case Some((resourceclass, config, variables, remainder)) ⇒
        staticresources.getOrElse(resourceclass, resourceclass.newInstance) match {
          case resource: BaseResource ⇒
            request.entity match {
              case None ⇒
              case Some(ContentEntity(_, length)) if request.method.entityallowed ⇒
              case Some(_) if !request.method.entityallowed ⇒ throw ServerError.`501`
              case _ ⇒
            }
            resource.handle(context ++ config ++ variables.getOrElse(Dispatcher.emptyvariables) ++ remainder)
          case _ ⇒ throw ServerError.`500`
        }
      case _ ⇒ throw ClientError.`404`
    }
  }

  final def init = {
    val servletcontexts = ServletContainer.getServletContexts
    templates = Templates(
      config.getConfigList("routes", List.empty).map { c: Config ⇒
        Template(c.getString("uri"), Class.forName(c.getString("resource-class-name")), c.getConfig("resource-config", ConfigFactory.empty))
      } ++ servletcontexts.flatMap { servletcontext ⇒
        servletcontext.getHttpServlets.map {
          case (httpservlet, servletconfig) ⇒
            Template(
              servletcontext.getContextPath.drop(1) + servletcontext.getServletMappings.getOrElse(servletconfig.getServletName, ""),
              httpservlet.getClass,
              ConfigFactory.empty)
        }
      } ++ servletcontexts.filter(_.getServlets.size == 1).map { servletcontext ⇒
        val servletclass = servletcontext.getHttpServlets.head._1.getClass
        Template(servletcontext.getContextPath.drop(1), servletclass, ConfigFactory.empty)
      } ++ servletcontexts.map { servletcontext ⇒
        val root = servletcontext.getRealPath.replace(".war", "")
        val rootclasses = root + "/WEB-INF/classes"
        val config = s"""{ roots = [ $root, $rootclasses ] }"""
        Template(
          servletcontext.getContextPath.drop(1) + "/*",
          classOf[resource.DirectoryResource],
          ConfigFactory.parseString(config))
      } ++ servletcontexts.filter(_.getServlets.size == 1).map { servletcontext ⇒
        val servletpath = servletcontext.getServletMappings.getOrElse(servletcontext.getHttpServlets.toSeq.head._2.getServletName, "")
        val root = servletcontext.getRealPath.replace(".war", "")
        val rootclasses = root + "/WEB-INF/classes"
        val config = s"""{ roots = [ $root, $rootclasses ] }"""
        Template(
          servletcontext.getContextPath.drop(1) + servletpath + "/*",
          classOf[resource.DirectoryResource],
          ConfigFactory.parseString(config))
      }).getOrElse(null)
    staticresources = (config.getConfigList("routes", List.empty).map { c: Config ⇒
      val resourceclass = Class.forName(c.getString("resource-class-name"))
      if (isStatic(resourceclass)) {
        val resource = resourceclass.newInstance.asInstanceOf[StaticResource]
        resource.init(c.getConfig("resource-config", ConfigFactory.empty))
        (resourceclass, resource)
      } else (null, null)
    } ++ servletcontexts.flatMap {
      _.getHttpServlets.map {
        case (httpservlet, _) ⇒ (httpservlet.getClass, new HttpServletResource(httpservlet))
      }
    }).filter(_._1 != null).toMap
    debug("name = " + name)
    if (null != templates) templates.toString.split("\n").filter(0 < _.length).foreach(r ⇒ debug("route = " + r))
  }

  @inline private[this] final def isStatic(resourceclass: Class[_]) = classOf[StaticResource].isAssignableFrom(resourceclass)

  private[this] final var templates: Templates = null

  private[this] final var staticresources: Map[Class[_], StaticResource] = null

}

/**
 *
 */
object Dispatcher {

  final val emptyvariables: http.Request.Variables = Map.empty

}

/**
 *
 */
final class DefaultDispatcher

  extends Dispatcher
