package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._

import json._
import xml._
import logging.HasLogger

import http.{ Request, Response, Status }
import http.Entity
import http.Entity._
import http.Method
import http.Method.{ DELETE, GET, HEAD, POST, PUT }
import http.Status.{ ClientError, Success }
import Matching._

/**
 *
 */
trait Resource

  extends BaseUniform

  with DelayedInit

  with HasLogger {

  import Resource._

  override final def delayedInit(init: ⇒ Unit): Unit = {
    resourcemethods.get(getClass) match {
      case Some(methods) ⇒ this.methods = methods
      case None ⇒ methods = Map.empty; init; resourcemethods = resourcemethods ++ Map(getClass -> methods)
    }
  }

  override final def completed(response: Response, context: Context) = {
    try {
      threadlocal.set(context ++ response)
      context.methodbody.completed match {
        case Some(completed) ⇒ completed(response)
        case _ ⇒
      }
    } finally {
      threadlocal.remove
    }
    super.completed(response, context)
  }

  override final def failed(e: Throwable, context: Context) = {
    try {
      threadlocal.set(context ++ e)
      context.methodbody.failed match {
        case Some(failed) ⇒ failed(e)
        case _ ⇒
      }
    } finally {
      threadlocal.remove
    }
    super.failed(e, context)
  }

  final def completed(response: Response): Unit = completed(response, threadlocal.get)

  final def completed(status: Status): Unit = completed(Response(status), threadlocal.get)

  final def failed(e: Throwable): Unit = failed(e, threadlocal.get)

  final def handle(request: Request, context: Context): Nothing = {
    import request._
    matchBody(method, entity, None) match {
      case Some((methodbody, input)) ⇒
        try {
          threadlocal.set(context ++ methodbody ++ request ++ Response(Success.`200`))
          methodbody.body(input)
          completed(response, context)
        } finally {
          threadlocal.remove
        }
      case None ⇒ throw ClientError.`415`
    }
  }

  final def Post[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add[E, A](POST, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Post[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](POST, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Put[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add[E, A](PUT, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Delete[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](DELETE, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](GET, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: Map[String, String] ⇒ A): MethodBody = {
    add[Map[String, String], A](GET, Some(typeOf[Map[String, String]]), Some(typeOf[A]), body)
  }

  final def Head(body: ⇒ Unit): MethodBody = {
    add[Unit, Unit](HEAD, None, None, (_: Unit) ⇒ body)
  }

  final def Head(body: Map[String, String] ⇒ Unit): MethodBody = {
    add[Map[String, String], Unit](HEAD, Some(typeOf[Map[String, String]]), None, body)
  }

  protected[this] final def request = threadlocal.get.request

  protected[this] final def response = threadlocal.get.response

  protected[this] final def context = threadlocal.get

  private[this] final def matchBody(method: Method, in: Option[Entity], out: Option[Entity]): Option[(MethodBody, Any)] = methods.get(method) match {
    case Some(inmethods) ⇒ in match {
      case Some(array: ArrayEntity) ⇒
        var input: Option[Any] = None
        In.get(array.contenttype.mimetype) match {
          case Some(intypes) ⇒
            println(inmethods.zip(intypes))
            inmethods.zip(intypes).find {
              case ((Some(methodtype), out), (intype, decode)) ⇒
                methodtype <:< intype && {
                  try {
                    input = Some(decode match {
                      case decode: ArrayEntityMarshaledDecoder[_] ⇒ decode(array, ClassTag(Class.forName(methodtype.toString)))
                      case decode: ArrayEntityDecoder[_] ⇒ decode(array)
                    })
                    true
                  } catch { case e: Throwable ⇒ warning("Decoding failed : " + e); false }
                }
              case _ ⇒ false
            } match {
              case Some(e) ⇒ Some((e._1._2.toList.head._2, input.getOrElse(())))
              case _ ⇒ None
            }
          case _ ⇒ None
        }
      case Some(entity) ⇒ None
      case _ ⇒ println(inmethods.get(None)); None
    }
    case _ ⇒ throw ClientError.`405`
  }

  private[this] final def add[E, A](method: Method, in: Option[Type], out: Option[Type], body: Body[E, A]): MethodBody = {
    val methodbody = MethodBody(body.asInstanceOf[Body[Any, Any]])
    methods = methods ++ (methods.get(method) match {
      case None ⇒ Map(method -> Map(in -> Map(out -> methodbody)))
      case Some(inout) ⇒ inout.get(in) match {
        case None ⇒ Map(method -> (inout ++ Map(in -> Map(out -> methodbody))))
        case Some(outbody) ⇒ Map(method -> (inout ++ Map(in -> (outbody ++ Map(out -> methodbody)))))
      }
    })
    methodbody
  }

  private[this] final var methods: Methods = null

}

/**
 * Singleton access to all Resources' methods maps.
 */
object Resource {

  final class MethodBody private (

    val body: Body[Any, Any],

    var completed: Option[Response ⇒ Unit],

    var failed: Option[Throwable ⇒ Unit]) {

    @inline final def onComplete(body: Response ⇒ Unit) = { completed = Some(body); this }

    @inline final def onFailure(body: Throwable ⇒ Unit) = { failed = Some(body); this }

  }

  object MethodBody {

    @inline def apply(body: Body[Any, Any]) = new MethodBody(body, None, None)

  }

  private type Body[E, A] = E ⇒ A

  private type Methods = Map[Method, In]

  private[rest]type In = Map[Option[Type], Out]

  private[rest]type Out = Map[Option[Type], MethodBody]

  private final var resourcemethods: Map[Class[_ <: Resource], Methods] = Map.empty

  private final val threadlocal = new ThreadLocal[Context]

}
