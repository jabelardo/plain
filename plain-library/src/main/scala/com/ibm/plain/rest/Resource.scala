package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._

import json._
import xml._
import logging.HasLogger
import reflect.tryBoolean

import http.{ Request, Response, Status, Entity, Method, MimeType }
import http.Entity._
import http.MimeType._
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

  override final def delayedInit(initialize: ⇒ Unit): Unit = {
    resourcemethods.get(getClass) match {
      case Some(methods) ⇒ this.methods = methods
      case None ⇒
        methods = Map.empty
        initialize
        methods = methods.map { case (method, Left(bodies)) ⇒ (method, Right(resourcePriorities(bodies))) case _ ⇒ null }
        if (log.isDebugEnabled) methods.foreach { case (method, Right(prios)) ⇒ debug(method + " " + prios.toList.size + " " + prios.toList) case _ ⇒ }
        resourcemethods = resourcemethods ++ Map(getClass -> methods)
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
    methods.get(request.method) match {
      case Some(Right(resourcepriorities)) ⇒ matching(request, context, resourcepriorities)
      case _ ⇒ throw ClientError.`405`
    }
  }

  final def Post[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add[E, A](POST, typeOf[E], typeOf[A], body)
  }

  final def Post[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](POST, typeOf[Unit], typeOf[A], (_: Unit) ⇒ body)
  }

  final def Put[E: TypeTag, A: TypeTag](body: E ⇒ A): MethodBody = {
    add[E, A](PUT, typeOf[E], typeOf[A], body)
  }

  final def Delete[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](DELETE, typeOf[Unit], typeOf[A], (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: ⇒ A): MethodBody = {
    add[Unit, A](GET, typeOf[Unit], typeOf[A], (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: Map[String, String] ⇒ A): MethodBody = {
    add[Map[String, String], A](GET, typeOf[Map[String, String]], typeOf[A], body)
  }

  final def Head(body: ⇒ Any): MethodBody = {
    add[Unit, Unit](HEAD, typeOf[Unit], typeOf[Unit], (_: Unit) ⇒ { body; () })
  }

  final def Head(body: Map[String, String] ⇒ Any): MethodBody = {
    add[Map[String, String], Unit](HEAD, typeOf[Map[String, String]], typeOf[Unit], (m: Map[String, String]) ⇒ { body(m); () })
  }

  protected[this] final def request = threadlocal.get.request

  protected[this] final def response = threadlocal.get.response

  protected[this] final def context = threadlocal.get

  /**
   * The most important method in this class.
   */
  private[this] final def matching(request: Request, context: Context, resourcepriorities: ResourcePriorities): Nothing = {

    val inentity: Option[Entity] = request.entity
    val inmimetype: MimeType = inentity match { case Some(entity: Entity) ⇒ entity.contenttype.mimetype case _ ⇒ `application/x-scala-unit` }
    val outmimetypes: List[MimeType] = List(`text/plain`) // get from Accept-Header

    var innerresult: Option[(MethodBody, Type, Type, Type, Type)] = None
    var innerinput: Option[Any] = None

    def tryDecode(in: Type, intype: Type) = decoders.get(intype) match {
      case Some(decode: Decoder[_]) ⇒ tryBoolean(innerinput = Some(decode(inentity)))
      case Some(decode: MarshaledDecoder[_]) ⇒ tryBoolean(innerinput = Some(decode(inentity, ClassTag(Class.forName(in.toString)))))
      case _ ⇒ false
    }

    def inner(outmimetype: MimeType) = resourcepriorities.collectFirst {
      case ((inoutmimetype, (intype, outtype)), ((in, out), methodbody)) if inoutmimetype == (inmimetype, outmimetype) && tryDecode(in, intype) ⇒
        innerresult = Some((methodbody, in, out, intype, outtype)); innerresult
    }

    outmimetypes.collectFirst {
      case outmimetype if inner(outmimetype).isDefined ⇒ innerresult
    } match {
      case Some(Some((methodbody, in, out, intype, outtype))) ⇒ try {
        threadlocal.set(context ++ methodbody ++ request ++ Response(Success.`200`))
        completed(response ++ encoders.get(outtype).get(innerinput match {
          case Some(input) ⇒ methodbody.body(input)
          case _ ⇒ throw ClientError.`415`
        }), threadlocal.get)
      } finally threadlocal.remove
      case _ ⇒ throw ClientError.`415`
    }
  }

  private[this] final def resourcePriorities(methodbodies: MethodBodies): ResourcePriorities = for {
    p ← (priorities.filter { case (_, (intype, outtype)) ⇒ methodbodies.exists { case ((in, out), _) ⇒ in <:< intype && out <:< outtype } }.toArray)
    m ← methodbodies if m._1._1 <:< p._2._1 && m._1._2 <:< p._2._2
  } yield (p, m)

  private[this] final def add[E, A](method: Method, in: Type, out: Type, body: Body[E, A]): MethodBody = {
    val methodbody = MethodBody(body.asInstanceOf[Body[Any, Any]])
    methods = methods ++ Map(method -> Left((methods.get(method) match {
      case None ⇒ Array(((in, out), methodbody))
      case Some(Left(bodies)) ⇒ bodies ++ Array(((in, out), methodbody))
      case _ ⇒ null
    })))
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

  private type Methods = Map[Method, Either[MethodBodies, ResourcePriorities]]

  private type MethodBodies = Array[((Type, Type), MethodBody)]

  private type ResourcePriorities = Array[(((MimeType, MimeType), (Type, Type)), ((Type, Type), MethodBody))]

  private final var resourcemethods: Map[Class[_ <: Resource], Methods] = Map.empty

  private final val threadlocal = new ThreadLocal[Context]

}
