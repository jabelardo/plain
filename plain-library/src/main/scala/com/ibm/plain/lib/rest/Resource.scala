package com.ibm.plain

package lib

package rest

import java.nio.charset.Charset
import text.UTF8
import http.Status._
import http.Entity._
import http.{ Entity, Request, Status }
import Resource._
import com.ibm.plain.lib.http.Response
import com.ibm.plain.lib.logging.Logging
import com.ibm.plain.lib.logging.HasLogger
import scala.annotation.tailrec
import com.ibm.plain.lib.http.Method
import com.ibm.plain.lib.http.Message.Headers

/**
 * The classic rest resource.
 */
trait Resource {

  def request: Request

  def get(entity: Option[Entity]): (Status, Option[Entity])

  def head(entity: Option[Entity]): Status

  def post(entity: Option[Entity]): (Status, Option[Entity])

  def put(entity: Option[Entity]): (Status, Option[Entity])

  def delete(entity: Option[Entity]): (Status, Option[Entity])

  def options(entity: Option[Entity]): (Status, Option[Entity])

  def connect(entity: Option[Entity]): Status

  def trace(entity: Option[Entity]): (Status, Entity)

  /**
   * convenience methods for the most common entity types.
   */
  def get: (Status, Option[Entity])

  def head: Status

  def post: (Status, Option[Entity])

  def post(s: String): (Status, Option[Entity])

  def put: (Status, Option[Entity])

  def put(s: String): (Status, Option[Entity])

  def delete: (Status, Option[Entity])

  def delete(s: String): (Status, Option[Entity])

}

/**
 * A basic implementation of Resource, containing plain ReST DSL.
 */
abstract class PlainResource(val path: String)

  extends Resource

  with HasLogger {

  type MMap[A, B] = scala.collection.mutable.Map[A, B]

  val methods: Map[Method, MMap[Headers, ResourceMethod]] = Map(
    Method.GET -> scala.collection.mutable.Map.empty,
    Method.HEAD -> scala.collection.mutable.Map.empty) // TODO: Add other Methods...

  def get(entity: Option[Entity]): (Status, Option[Entity]) = {
    entity match {
      case Some(e) =>
        e
      case None =>

    }
    Ok("Passt scho.")
  }

  private def registerMethod(method: Method, headers: Headers, f: ResourceMethodImpl) = {
    val rm = ResourceMethod(f, None)
    
    methods.get(method).get.put(headers, rm) match {
      case Some(method) =>
        warning("Registered more the one Method for HTTP-Method " + method + " and headers: " + headers + ".")
      case None =>
    }
    
    rm
  }

  /*
   * plain ReST DSL methods are going here...
   */
  def onGET(header: Map[String, String])(f: ResourceMethodImpl) = registerMethod(Method.GET, header, f)

  /*
   * Some helper classes
   */
  type ResourceMethodImpl = (ReqResEntVar => (Status, Option[Entity]))

  case class ResourceMethod(_execute: ResourceMethodImpl, next: Option[ResourceMethod]) {

    @tailrec val execute: ResourceMethodImpl = in => try {
      this._execute(in)
    } catch {
      case e: Exception =>
        next match {
          case Some(next) =>
            debug("ResourceMethod failed with exception: " + e.getMessage + ". Try next.")
            next.execute(in)
          case None =>
            val msg = "ResourceMethod failed with exception: " + e.getMessage
            warning(msg)
            ServerException(msg)
        }
    }

    def ||(f: ResourceMethodImpl): ResourceMethod = next match {
      case Some(next) =>
        ResourceMethod(_execute, Some(next || f))
      case None =>
        ResourceMethod(_execute, Some(ResourceMethod(f, None)))
    }

  }

  case class ReqResEntVar(val request: Request, val response: Response, val entity: Option[Entity], val vars: Option[Map[String, String]])

  sealed abstract class Placeholder[A]

  object Request extends Placeholder[Request]

  object Response extends Placeholder[Response]

  object Entity extends Placeholder[Entity]

  object URLParams extends Placeholder[List[String]]

  object asInt {
    def unapply(s: String): Option[Int] = {
      try {
        Some(s.toInt)
      } catch {
        case e: Exception => None
      }
    }
  }

  def g[Z](arg: Placeholder[Z])(implicit params: ReqResEntVar): Z = params match {
    case ReqResEntVar(req, res, ent, vars) =>
      arg match {
        case Request => req
        case Response => res
        case Entity => ent.get
        case URLParams => List("Eins")
      }
  }

  def use[A](arg0: Placeholder[A])(f: PartialFunction[A, (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params => f(g(arg0))

  def use[A, B](arg0: Placeholder[A], arg1: Placeholder[B])(f: PartialFunction[(A, B), (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params => f(g(arg0), g(arg1))

  def use[A, B, C](arg0: Placeholder[A], arg1: Placeholder[B], arg2: Placeholder[C])(f: PartialFunction[(A, B, C), (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params => f(g(arg0), g(arg1), g(arg2))

  def use[A, B, C, D](arg0: Placeholder[A], arg1: Placeholder[B], arg2: Placeholder[C], arg3: Placeholder[D])(f: PartialFunction[(A, B, C, D), (Status, Option[Entity])]): ResourceMethodImpl =
    implicit params => f(g(arg0), g(arg1), g(arg2), g(arg3))

  /*
   * Test methods
   */
  onGET(Map.empty) {
    use(Response, Entity, URLParams) {
      case (req, entity, List(asInt(s1), s2)) =>
        Ok("HUHU")
    }
  } || { p =>
    Ok("Oder so")
  } || { p =>
    Ok("Und so weiter...")
  }

}

/**
 * A basic implementation of Resource.
 */
abstract class BaseResource

  extends Resource {

  final var request: Request = null

  def get(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def head(entity: Option[Entity]): Status = get(entity)._1

  def post(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def put(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def delete(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def options(entity: Option[Entity]): (Status, Option[Entity]) = (ClientError.`405`, None)

  def connect(entity: Option[Entity]): Status = ClientError.`405`

  def trace(entity: Option[Entity]): (Status, Entity) = (Success.`200`, BytesEntity(request.toString.getBytes(UTF8)))

  def get: (Status, Option[Entity]) = get(None)

  def head: Status = head(None)

  /**
   * All details in the query.
   */
  def post: (Status, Option[Entity]) = post(None)

  def post(s: String): (Status, Option[Entity]) = post(Some(BytesEntity(s.getBytes(UTF8))))

  /**
   * All details in the query.
   */
  def put: (Status, Option[Entity]) = put(None)

  def put(s: String): (Status, Option[Entity]) = put(Some(BytesEntity(s.getBytes(UTF8))))

  def delete: (Status, Option[Entity]) = delete(None)

  def delete(s: String): (Status, Option[Entity]) = delete(Some(BytesEntity(s.getBytes(UTF8))))
}

/**
 * Often used helpers for users of this class.
 */
object Resource {

  final def Ok(s: String): (Status, Option[Entity]) = Ok(s, UTF8)

  final def Ok(s: String, cset: Charset) = (Success.`200`, Some(BytesEntity(s.getBytes(cset))))

  final def ServerException(s: String): (Status, Option[Entity]) = ServerException(s, UTF8)

  final def ServerException(s: String, cset: Charset) = (Status.ServerError.`501`, Some(BytesEntity(s.getBytes(cset))))

}

