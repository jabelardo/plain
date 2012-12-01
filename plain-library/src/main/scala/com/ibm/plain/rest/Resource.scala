package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.language.implicitConversions

import aio.FileByteChannel.forWriting
import aio.transfer
import http.{ Entity, Request, Response, Method, Status }
import http.Entity.ContentEntity
import http.Method._
import http.Status._

/**
 *
 */
trait Resource

  extends BaseUniform

  with DelayedInit {

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
    methods.get(method) match {
      case Some(inout) ⇒
        // transfer-decoding here
        input(request) match {
          case Some(in) ⇒ in match {
            case Right(ContentEntity(length)) if length <= maxEntityBufferSize ⇒
            case Left(query) ⇒
            case _ ⇒ println("no correct input")
          }
          case None ⇒ println("no input")
        }
        val methodbody = resourcemethods.get(this.getClass).get.get(method).toList.head.get(None).get.toList.head._2
        println(methodbody)
        try {
          threadlocal.set(context ++ methodbody ++ request ++ Response(Success.`200`))
          methodbody.body(())
          completed(response, context)
        } finally {
          threadlocal.remove
        }
      case None ⇒ throw ClientError.`405`
    }
  }

  def test = ()

  final def Post[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add(POST, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Post[A: TypeTag](body: ⇒ A): MethodBody = {
    add(POST, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Put[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add(PUT, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Delete[A: TypeTag](body: ⇒ A): MethodBody = {
    add(DELETE, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: ⇒ A): MethodBody = {
    add(GET, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: Map[String, String] ⇒ A): MethodBody = {
    add(GET, Some(typeOf[Map[String, String]]), Some(typeOf[A]), body)
  }

  final def Head(body: ⇒ Unit): MethodBody = {
    add(HEAD, None, None, (_: Unit) ⇒ body)
  }

  protected[this] final def request = threadlocal.get.request

  protected[this] final def response = threadlocal.get.response

  protected[this] final def context = threadlocal.get

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

  private[this] final def input(request: Request): Option[Either[String, Entity]] = request.entity match {
    case Some(entity) ⇒ Some(Right(entity))
    case None ⇒ request.query match {
      case Some(query) ⇒ Some(Left(query))
      case None ⇒ None
    }
  }

  def m = methods // for TestResource

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

  private type Methods = Map[Method, InOut]

  private type InOut = Map[Option[Type], OutMethodBody]

  private type OutMethodBody = Map[Option[Type], MethodBody]

  private final var resourcemethods: Map[Class[_ <: Resource], Methods] = Map.empty

  private final val threadlocal = new ThreadLocal[Context]

}
