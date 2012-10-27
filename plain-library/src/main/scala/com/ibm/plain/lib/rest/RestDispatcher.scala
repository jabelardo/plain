package com.ibm.plain

package lib

package rest

import text.UTF8
import logging.HasLogger
import http._
import http.Entity._
import http.Status._

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class RestDispatcher

  extends HttpDispatcher

  with HasLogger {

  def dispatch(request: Request): Option[Response] = {

    // find the REST services classes now and dispatch to the right instance to the right method with all path variables set.

    //    resources.get(request.path) match {
    //      case Some(resourceclass) ⇒
    //        resourceclass.newInstance match {
    //          case resource: BaseResource ⇒
    //            resource.request = request
    //            Some(Response(resource.get))
    //          case c ⇒
    //            error("Class must inherit from BaseRequest : " + c)
    //            throw ServerError.`501`
    //        }
    //      case None ⇒ throw ClientError.`404`
    //    }

    t.get(request.path) match {
      case None ⇒ throw ClientError.`404`
      case Some((resourceclass, _)) ⇒
        val r = resourceclass.newInstance.asInstanceOf[BaseResource]
        Some(Response(r.get._1))
    }
  }

  def register(path: Request.Path, clazz: Class[Resource]) = resources += ((path, clazz))

  private[this] final val resources = new scala.collection.mutable.HashMap[Request.Path, Class[Resource]]

  register(List("ping"), Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]])

  val resourceclass = Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]]

  val f = Template("system/division/{division}", resourceclass)
  val a = Template("system/division/{division}/department/{department}", resourceclass)
  val b = Template("system/division/{division}/manager/{manager}", resourceclass)
  val g = Template("system/division/{division}/manager/{manager}/{salery}", resourceclass)
  val c = Template("system/location/{location}", resourceclass)
  val d = Template("user/{user}", resourceclass)
  val e = Template("ping", resourceclass)

  //    println(f)
  //    println(a)
  //    println(b)
  //    println(c)
  //    println(d)
  //    println(e)

  require(Templates(a, b, c, d, e, f, g).toString == Templates(g, f, e, d, c, b, a).toString)
  require(Templates(a, c, e, d, f, g, b).toString == Templates(g, e, d, f, c, b, a).toString)
  //  println(Templates(g, e, d, f, c, b, a))

  val t = Templates(g, e, d, f, c, b, a)

  //  println(t.get(List("")))
  //  println(t.get("system/division/FIS".split("/").toList))
  //  println(t.get("system/division/FIS/department/F01".split("/").toList))
  //  println(t.get("system/division/FIS/manager/Hahn/0".split("/").toList))
  //  println(t.get("user/Michael".split("/").toList))
  //  println(t.get("user////Michael/".split("/").toList))
  //  println(t.get("user/Michael/X".split("/").toList))
  //  println(t.get("ping".split("/").toList))

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultRestDispatcher

  extends RestDispatcher {

  override def dispatch(request: Request): Option[Response] = super.dispatch(request) match {
    case None ⇒ Some(Response(Status.ServerError.`501`))
    case e ⇒ e
  }

}
