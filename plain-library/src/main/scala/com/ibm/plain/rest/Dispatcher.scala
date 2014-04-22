package com.ibm

package plain

package rest

import com.ibm.plain.aio.{ Exchange, ExchangeHandler }
import com.ibm.plain.http.HttpDispatcher
import com.typesafe.config.{ Config, ConfigFactory }

import config.config2RichConfig
import http.{ Entity, Request, Response }
import http.Status.{ ClientError, ServerError, Success }
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
    val response = Response(exchange.writeBuffer, Success.`200`)
    exchange ++ response
    val context = exchange.attachment match {
      case Some(context) ⇒ context
      case _ ⇒
        val context = new Context(request)
        exchange ++ Some(context)
        context
    }
    context ++ request ++ response
    try templates.get(request.method, request.path) match {
      case Some((resourceclass, config, variables, remainder)) ⇒
        staticresources.getOrElse(resourceclass, resourceclass.newInstance) match {
          case resource: Uniform ⇒
            request.entity match {
              case None ⇒
              case Some(Entity(_, _, _)) if request.method.entityallowed ⇒
              case Some(Entity(_, length, _)) if !request.method.entityallowed && length < aio.defaultBufferSize ⇒
              case Some(_) if !request.method.entityallowed ⇒ throw ClientError.`413`
              case _ ⇒
            }
            context ++ config ++ variables.getOrElse(emptyvariables) ++ remainder
            resource.process(exchange, handler)
          case _ ⇒ throw ServerError.`500`
        }
      case _ ⇒ throw ClientError.`404`
    } catch {
      case e: Throwable ⇒ failed(e, exchange, handler)
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
        val resource = resourceclass.newInstance.asInstanceOf[StaticUniform]
        resource.init(c.getConfig("resource-config", ConfigFactory.empty))
        (resourceclass, resource)
      } else (null, null)
    } ++ servletcontexts.flatMap {
      _.getHttpServlets.map {
        case e @ (either, _, _) ⇒ ((if (either.isLeft) either.left.get._2 else either.right.get).getClass, new HttpServletResource(e))
      }
    }).filter(_._1 != null).toMap

    debug("name = " + name)
    debug("staticresources = " + staticresources.keySet)
    if (null != templates) templates.toString.split("\n").filter(0 < _.length).foreach(r ⇒ debug("route = " + r)) else warn("No routes defined.")
    this
  }

  @inline private[this] final def isStatic(resourceclass: Class[_]) = classOf[StaticUniform].isAssignableFrom(resourceclass)

  private[this] final var templates: Templates = null

  private[this] final var staticresources: Map[Class[_], StaticUniform] = null

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
