package com.ibm

package plain

package rest

import com.ibm.plain.aio.{ Exchange, ExchangeHandler }
import com.ibm.plain.http.HttpDispatcher
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.concurrent.TrieMap

import aio.{ Exchange, ExchangeHandler }
import config.config2RichConfig
import http.{ Entity, HttpDispatcher, Request }
import http.Status.{ ClientError, ServerError }
import servlet.ServletContainer
import servlet.http.HttpServletResource

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class Dispatcher

  extends HttpDispatcher[Context] {

  import Dispatcher._

  final def process(exchange: Exchange[Context], handler: ExchangeHandler[Context]) = {
    val request = exchange.inMessage.asInstanceOf[Request]
    val context = exchange.attachment match {
      case Some(context) ⇒ context
      case _ ⇒
        val context = Context()
        exchange ++ Some(context)
        context ++ request
    }
    if (null != resource) {
      resource.process(exchange, handler)
    } else {
      templates.get(request.method, request.path) match {
        case Some((resourceclass, config, variables, remainder)) ⇒
          staticresources.getOrElse(resourceclass, resourceclass.newInstance) match {
            case resource: BaseResource ⇒
              request.entity match {
                case None ⇒
                case Some(Entity(_)) if request.method.entityallowed ⇒
                case Some(Entity(_, length, _)) if !request.method.entityallowed && length < aio.defaultBufferSize ⇒
                case Some(_) if !request.method.entityallowed ⇒ throw ClientError.`413`
                case _ ⇒
              }
              context ++ config ++ variables.getOrElse(emptyvariables) ++ remainder
              this.resource = resource
              resource.process(exchange, handler)
            case _ ⇒ throw ServerError.`500`
          }
        case _ ⇒ throw ClientError.`404`
      }
    }
  }

  final def init = {
    val servletcontexts = ServletContainer.getServletContexts

    templates = Templates(
      config.getConfigList("routes", List.empty).map { c: Config ⇒
        Template(c.getString("uri"), Class.forName(c.getString("resource-class-name")), c.getConfig("resource-config", ConfigFactory.empty))
      } ++ servletcontexts.flatMap { servletcontext ⇒
        servletcontext.getHttpServlets.map {
          case (either, servletconfig, scontext) ⇒ Template(
            servletcontext.getContextPath.drop(1) + servletcontext.getServletMappings.getOrElse(servletconfig.getServletName, ""),
            (if (either.isLeft) either.left.get._2 else either.right.get).getClass,
            ConfigFactory.empty)
        }
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
        case e @ (either, _, _) ⇒ ((if (either.isLeft) either.left.get._2 else either.right.get).getClass, new HttpServletResource(e))
      }
    }).filter(_._1 != null).toMap

    debug("name = " + name + " " + staticresources)
    if (null != templates) templates.toString.split("\n").filter(0 < _.length).foreach(r ⇒ debug("route = " + r)) else warn("No routes defined.")
    this
  }

  @inline private[this] final def isStatic(resourceclass: Class[_]) = classOf[StaticResource].isAssignableFrom(resourceclass)

  private[this] final var templates: Templates = null

  private[this] final var staticresources: Map[Class[_], StaticResource] = null

  private[this] final val requestresources = new TrieMap[Request, BaseResource]

  private[this] final var resource: BaseResource = null

}

/**
 *
 */
object Dispatcher {

  private final val emptyvariables: http.Request.Variables = Map.empty

}

/**
 *
 */
final class DefaultDispatcher

  extends Dispatcher
