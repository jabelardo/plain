package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.language.implicitConversions

import aio.FileByteChannel.forWriting
import aio.transfer
import http.{ Request, Response, Method }
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

  def handle(request: Request, context: Context): Nothing = {
    test
    request.entity match {
      case Some(entity) ⇒
        http.Header.Entity.`Content-Type`(request.headers) match {
          case Some(value) ⇒ println(value)
          case _ ⇒ println("no ct")
        }
      case _ ⇒
    }
    val body = methods.get(request.method) match {
      case Some(m) ⇒ m.toList.head._2.toList.head._2
      case None ⇒ throw ClientError.`405`
    }
    val in: Option[Any] = request.entity match {
      case Some(ContentEntity(length)) if length <= maxEntityBufferSize ⇒ None
      case _ ⇒ None
    }
    val response = Response(Success.`200`)
    val r = body(in.getOrElse(()))(request, response, context)
    println(r)
    completed(response, context)
  }

  def test = ()

  final def Post[E: TypeTag, A: TypeTag](body: E ⇒ A): Unit = {
    //    add(POST, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Post[A: TypeTag](body: ⇒ A): Unit = {
    //    add(POST, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Put[E: TypeTag, A: TypeTag](body: E ⇒ A): Unit = {
    //    add(PUT, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Delete[A: TypeTag](body: ⇒ A): Unit = {
    //    add(DELETE, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  //  final def Get[A: TypeTag](body: ⇒ A): Unit = {
  //    //    add(GET, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  //  }

  final def Get[A: TypeTag](body: ⇒ (Request, Response, Context) ⇒ A): Unit = {
    add(GET, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: ⇒ A): Unit = {
    add(GET, None, Some(typeOf[A]), (_: Unit) ⇒ (_: Request, _: Response, _: Context) ⇒ body)
  }

  //  final def Get[A: TypeTag](body: Map[String, String] ⇒ A): Unit = {
  //    //    add(GET, Some(typeOf[Map[String, String]]), Some(typeOf[A]), body)
  //  }

  final def Head(body: ⇒ Unit): Unit = {
    //    (HEAD, None, None, (_: Unit) ⇒ body)
  }

  private[this] final def add[E, A](method: Method, in: Option[Type], out: Option[Type], body: Body[E, A]) = {
    val b = body.asInstanceOf[Body[Any, Any]]
    methods = methods ++ (methods.get(method) match {
      case None ⇒ methods ++ Map(method -> Map(in -> Map(out -> b)))
      case Some(inout) ⇒ inout.get(in) match {
        case None ⇒ methods ++ Map(method -> (inout ++ Map(in -> Map(out -> b))))
        case Some(outbody) ⇒ methods ++ Map(method -> (inout ++ Map(in -> (outbody ++ Map(out -> b)))))
      }
    })
  }

  def m = methods

  private[this] final var methods: Methods = null

}

/**
 * Singleton access to all Resources' methods maps.
 */
object Resource {

  private type Body[E, A] = E ⇒ (Request, Response, Context) ⇒ A

  private type Methods = Map[Method, InOut]

  private type InOut = Map[Option[Type], OutBody]

  private type OutBody = Map[Option[Type], Body[Any, Any]]

  private final var resourcemethods: Map[Class[_ <: Resource], Methods] = Map.empty

}

