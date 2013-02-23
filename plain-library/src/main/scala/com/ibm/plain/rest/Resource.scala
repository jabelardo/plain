package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._
import json._
import xml._
import logging.HasLogger
import reflect.tryBoolean
import aio.Io
import http.{ Request, Response, Status, Entity, Method, MimeType, Accept }
import http.Entity._
import http.MimeType._
import http.Method.{ DELETE, GET, HEAD, POST, PUT }
import http.Status.{ ClientError, ServerError, Success }
import http.Header.Request.{ `Accept` ⇒ AcceptHeader }
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

  final def completed(context: Context) = {
    try {
      threadlocal.set(context)
      context.methodbody.completed match {
        case Some(completed) ⇒ completed(context.response)
        case _ ⇒
      }
      super.completed(context.response, context.io)
    } finally {
      threadlocal.remove
    }
  }

  final def failed(e: Throwable, context: Context) = {
    try {
      threadlocal.set(context ++ e)
      context.methodbody.failed match {
        case Some(failed) ⇒ failed(e)
        case _ ⇒
      }
      super.failed(e, context.io +++ context.response)
    } finally {
      threadlocal.remove
    }
  }

  final def process(io: Io): Unit = throw new UnsupportedOperationException

  final def handle(context: Context) = try {
    methods.get(context.request.method) match {
      case Some(Right(resourcepriorities)) ⇒ execute(context, resourcepriorities)
      case _ ⇒ throw ClientError.`405`
    }
  } catch {
    case e: Throwable ⇒ failed(e, context)
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

  final def Get[A: TypeTag](body: Form ⇒ A): MethodBody = {
    add[Form, A](GET, typeOf[Form], typeOf[A], body)
  }

  final def Head(body: ⇒ Any): MethodBody = {
    add[Unit, Unit](HEAD, typeOf[Unit], typeOf[Unit], (_: Unit) ⇒ { body; () })
  }

  final def Head(body: Form ⇒ Any): MethodBody = {
    add[Form, Unit](HEAD, typeOf[Form], typeOf[Unit], (m: Form) ⇒ { body(m); () })
  }

  protected[this] final def request = threadlocal.get.request

  protected[this] final def response = threadlocal.get.response

  protected[this] final def context = threadlocal.get

  /**
   * The most important method in this class.
   */
  private[this] final def execute(context: Context, resourcepriorities: ResourcePriorities) = try {
    threadlocal.set(context)

    val inentity: Option[Entity] = request.entity
    val inmimetype: MimeType = inentity match { case Some(entity: Entity) ⇒ entity.contenttype.mimetype case _ ⇒ `application/x-scala-unit` }
    val outmimetypes: List[MimeType] = AcceptHeader(request.headers) match {
      case Some(Accept(mimetypes)) ⇒ mimetypes
      case _ ⇒ List(`*/*`)
    }

    var innerinput: Option[(Any, AnyRef)] = None

    def tryDecode(in: Type, decode: AnyRef): Boolean = {
      if (innerinput.isDefined && innerinput.get._2 == decode) return true // avoid unnecessary calls, decode can be expensive
      decode match {
        case decode: Decoder[_] ⇒ tryBoolean(innerinput = Some((decode(inentity), decode)))
        case decode: MarshaledDecoder[_] ⇒ tryBoolean(innerinput = Some((decode(inentity, ClassTag(Class.forName(in.toString))), decode)))
        case _ ⇒ false
      }
    }

    (for {
      o ← outmimetypes
      r ← resourcepriorities
    } yield (o, r)).collectFirst {
      case (outmimetype, (inoutmimetype, (in, methodbody), (decode, encode))) if (inoutmimetype == ((inmimetype, outmimetype))) && (tryDecode(in, decode)) ⇒
        (methodbody, encode)
    } match {
      case Some((methodbody, encode)) ⇒
        val response = Response(request, Success.`200`)
        context ++ methodbody ++ response
        response ++ encode(innerinput match {
          case Some((input, _)) ⇒ methodbody.body(input)
          case _ ⇒ throw ServerError.`501`
        })
        completed(context)
      case _ ⇒ throw ClientError.`415`
    }
  } finally threadlocal.remove

  /**
   * This is ugly, but it's only called once per Resource and Method, the resulting data structure is very efficient.
   */
  private[this] final def resourcePriorities(methodbodies: MethodBodies): ResourcePriorities = {
    val matching = new Matching
    for {
      p ← matching.priorities.filter { case (_, (intype, outtype)) ⇒ methodbodies.exists { case ((in, out), _) ⇒ in <:< intype && out <:< outtype } }
      m ← methodbodies if m._1._1 <:< p._2._1 && m._1._2 <:< p._2._2
    } yield (p._1, (m._1._1, m._2), (matching.decoders.get(p._2._1).get, matching.encoders.get(p._2._2).get))
  }

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

    var completed: Option[Response ⇒ Any],

    var failed: Option[Throwable ⇒ Any]) {

    @inline final def onComplete(body: Response ⇒ Any) = { completed = Some(body); this }

    @inline final def onFailure(body: Throwable ⇒ Any) = { failed = Some(body); this }

  }

  object MethodBody {

    @inline def apply(body: Body[Any, Any]) = new MethodBody(body, None, None)

  }

  private type Body[E, A] = E ⇒ A

  private type Methods = Map[Method, Either[MethodBodies, ResourcePriorities]]

  private type MethodBodies = Array[((Type, Type), MethodBody)]

  private type ResourcePriority = ((MimeType, MimeType), (Type, MethodBody), (AnyRef, Encoder))

  private type ResourcePriorities = Array[ResourcePriority]

  private final var resourcemethods: Map[Class[_ <: Resource], Methods] = Map.empty

  private final val threadlocal = new ThreadLocal[Context]

}
