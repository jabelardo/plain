package com.ibm

package plain

package rest

import aio.FileByteChannel.forWriting
import aio.transfer
import text.UTF8
import http.Entity._
import http.Message.Headers
import http.Method._
import http.Status._
import http.{ Method, Request, Response, Status, Entity }
import logging.HasLogger

import scala.annotation.tailrec
import scala.language.implicitConversions
import java.nio.charset.Charset

/**
 *
 */
trait Resource extends BaseUniform {

  final def Ok(s: String): (Status, Option[Entity]) = Ok(s, UTF8)

  final def Ok(s: String, cset: Charset) = (Success.`200`, Some(BytesEntity(s.getBytes(cset))))

  final def ServerException(s: String): (Status, Option[Entity]) = ServerException(s, UTF8)

  final def ServerException(s: String, cset: Charset) = (Status.ServerError.`501`, Some(BytesEntity(s.getBytes(cset))))

}

/**
 *
 */
trait IsPlainDSLResource

  extends Resource

  with HasLogger {

  /*
   *  Some abstract values ...
   */
  val path: String

  /*
   * Some implicits ...
   */
  implicit def makeResourceMethodImpl(from: (Status, Option[Entity])): ResourceMethodImpl = p ⇒ from

  /*
   * Some inner classes ...
   */
  case class ResourceMethod(_execute: ResourceMethodImpl, next: Option[ResourceMethod]) {

    @tailrec val execute: ResourceMethodImpl = in ⇒ try {
      this._execute(in)
    } catch {
      case e: Exception ⇒
        next match {
          case Some(next) ⇒
            debug("ResourceMethod failed with exception: " + e.getMessage + ". Try next.")
            next.execute(in)
          case None ⇒
            val msg = "ResourceMethod failed with exception: " + e.getMessage
            warning(msg)
            ServerException(msg)
        }
    }

    def ||(f: ResourceMethodImpl): ResourceMethod = next match {
      case Some(next) ⇒
        ResourceMethod(_execute, Some(next || f))
      case None ⇒
        ResourceMethod(_execute, Some(ResourceMethod(f, None)))
    }

  }

  case class ReqEntVar(val request: Request, val entity: Option[Entity], val vars: List[String])

  sealed abstract class Placeholder[A]

  object Request extends Placeholder[Request]

  object Entity extends Placeholder[Entity]

  object URLParams extends Placeholder[List[String]]

  object asInt {
    def unapply(s: String): Option[Int] = {
      try {
        Some(s.toInt)
      } catch {
        case e: Exception ⇒ None
      }
    }
  }

  /*
   * Some type definitions ...
   */
  type ResourceMethodImpl = (ReqEntVar ⇒ (Status, Option[Entity]))
  type MMap[A, B] = scala.collection.mutable.Map[A, B]

  val methods: Map[Method, MMap[Headers, ResourceMethod]] = Map(
    GET -> scala.collection.mutable.Map.empty,
    HEAD -> scala.collection.mutable.Map.empty) // TODO: Add other Methods...

  /*
   * Some help methods for DSL
   */
  private def g[Z](arg: Placeholder[Z])(implicit params: ReqEntVar): Z = params match {
    case ReqEntVar(req, ent, vars) ⇒
      arg match {
        case Request ⇒ req
        case Entity ⇒ ent.get
        case URLParams ⇒ List("Eins")
      }
  }

  private def registerMethod(method: Method, headers: Headers, f: ResourceMethodImpl) = {
    val rm = ResourceMethod(f, None)

    methods.get(method).get.put(headers, rm) match {
      case Some(method) ⇒
        warning("Registered more the one Method for HTTP-Method " + method + " and headers: " + headers + ".")
      case None ⇒
    }

    rm
  }

  /*
   * The DSL itself
   */
  def use[A](arg0: Placeholder[A])(f: PartialFunction[A, (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params ⇒ f(g(arg0))

  def use[A, B](arg0: Placeholder[A], arg1: Placeholder[B])(f: PartialFunction[(A, B), (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params ⇒ f(g(arg0), g(arg1))

  def use[A, B, C](arg0: Placeholder[A], arg1: Placeholder[B], arg2: Placeholder[C])(f: PartialFunction[(A, B, C), (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params ⇒ f(g(arg0), g(arg1), g(arg2))

  def onGET(header: Map[String, String])(f: ResourceMethodImpl): ResourceMethod = registerMethod(Method.GET, header, f)
  
  def onGET(f: ResourceMethodImpl): ResourceMethod = onGET(Map.empty.asInstanceOf[Map[String, String]])(f)

  def onGET(header: (String, String) *)(f: ResourceMethodImpl): ResourceMethod = onGET(header.toMap)(f)
  /*
   * Uniform implementation
   */
  def handle(request: Request, context: Context): Nothing = request.method match {
    case method @ (POST | GET | HEAD) =>
      methods.get(method) match {
        case Some(map) => map.find({ case (headers, _) => headers.toList.intersect(request.headers.toList).size == headers.size }) match {
          case Some((_, resourceMethod)) =>
            completed(Response(resourceMethod.execute(ReqEntVar(request, request.entity, context.variables.map(_._2).toList))), context)
            handled
          case None => throw ClientError.`400` // No applicable header fond ...
        }

        case None =>
          throw ServerError.`500`
      }

    case _ ⇒ throw ClientError.`405`
  }
}

abstract class PlainDSLResource(val path: String) extends IsPlainDSLResource
