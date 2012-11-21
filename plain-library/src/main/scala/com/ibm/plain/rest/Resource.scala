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

import json._
import json.Json._
import xml._

/**
 *
 */
trait Resource

  extends BaseUniform

  with DelayedInit {

  import Resource._

  override final def delayedInit(body: ⇒ Unit): Unit = resourcemethods.get(getClass) match {
    case Some(methods) ⇒ this.methods = methods
    case None ⇒ println("added " + getClass); methods = new Methods; body; resourcemethods.put(getClass, methods)
  }

  def completed(response: Response): Nothing = completed(response, context)

  def failed(e: Throwable): Nothing = failed(e, context)

  def handle(request: Request, context: Context): Nothing = {
    request_ = request
    context_ = context
    //
    //    println("methods" + methods)
    //    println(resourcemethods)
    //    println(methods.get(POST).get.get(Some(typeOf[String])).get.get(Some(typeOf[String])).get("this is the input"))
    //    println(methods.get(POST).get.get(Some(typeOf[User])).get.get(Some(typeOf[User])).get(User("Mary", 7)))
    //    println(methods.get(POST).get.get(Some(typeOf[Map[String, String]])).get.get(Some(typeOf[Predef.String])).get(Map("a" -> "x", "b" -> "y")))
    //    println(methods.get(PUT).get.get(Some(typeOf[JObject])).get.get(Some(typeOf[Json])).get(Json.parse("{\"name\":\"value\"}").asObject))
    //    println(methods.get(GET).get.get(None).get.get(Some(typeOf[scala.xml.Elem])).get(()))
    //    println(new String(methods.get(GET).get.get(None).get.get(Some(typeOf[Array[Byte]])).get(()).asInstanceOf[Array[Byte]]))
    //    println(methods.get(GET).get.get(None).get.get(Some(typeOf[java.lang.String])).get(()))
    //    println(methods.get(GET).get.get(None).get.get(Some(typeOf[User])).get(()))
    //    println(methods.get(GET).get.get(None).get.get(Some(typeOf[JsonMarshaled])).get(()).asInstanceOf[JsonMarshaled].toJson)
    //    println(methods.get(GET).get.get(None).get.get(Some(typeOf[XmlMarshaled])).get(()).asInstanceOf[XmlMarshaled].toXml)
    //    println(methods.get(GET).get.get(Some(typeOf[Map[String, String]])).get.get(Some(typeOf[scala.collection.immutable.Map[String, String]])).get(Map("a" -> "x", "b" -> "y")))

    val methodbody: Body[Any, Any] = methods.get(request.method) match {
      case Some(method) ⇒ method.toList.head._2.toList.head._2
      case None ⇒ throw ClientError.`405`
    }
    val in: Option[Any] = request.entity match {
      case Some(ContentEntity(length, contenttype)) if length <= maxEntityBufferSize ⇒ None
      case _ ⇒ None
    }
    val out: Any = methodbody(in.getOrElse(()))
    // println("out " + out)
    completed(response)
  }

  final def Post[E: TypeTag, A: TypeTag](body: E ⇒ A): Unit = {
    add(POST, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Post[A: TypeTag](body: ⇒ A): Unit = {
    add(POST, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Put[E: TypeTag, A: TypeTag](body: E ⇒ A): Unit = {
    add(PUT, Some(typeOf[E]), Some(typeOf[A]), body)
  }

  final def Delete[A: TypeTag](body: ⇒ A): Unit = {
    add(DELETE, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  final def Get[A: TypeTag](body: ⇒ A): Unit = {
    add(GET, None, Some(typeOf[A]), (_: Unit) ⇒ body)
  }

  /**
   * called on a GET with an `application/x-www-form-urlencoded` query
   */
  final def Get[A: TypeTag](body: Map[String, String] ⇒ A): Unit = {
    add(GET, Some(typeOf[Map[String, String]]), Some(typeOf[A]), body)
  }

  final def Head(body: ⇒ Unit): Unit = {
    add(HEAD, None, None, (_: Unit) ⇒ body)
  }

  private[this] final def add[E, A](method: Method, in: Option[Type], out: Option[Type], body: Body[E, A]) = methods.get(method) match {
    case None ⇒
      val i = new InOut; val o = new OutBody; o.put(out, body.asInstanceOf[Body[Any, Any]]); i.put(in, o); methods.put(method, i)
    case Some(i) ⇒ i.get(in) match {
      case None ⇒
        val o = new OutBody; o.put(out, body.asInstanceOf[Body[Any, Any]]); i.put(in, o)
      case Some(o) ⇒
        o.put(out, body.asInstanceOf[Body[Any, Any]])
    }
  }

  protected[this] def request = request_

  protected[this] def context = context_

  protected[this] final val response = Response(Success.`200`)

  private[this] final var request_ : Request = null

  private[this] final var context_ : Context = null

  private[this] final var methods: Methods = null

}

/**
 * Singleton access to all Resources' methods maps.
 */
object Resource {

  type Body[E, A] = E ⇒ A

  private type MutableMap[E, A] = scala.collection.mutable.HashMap[E, A]

  private type Methods = MutableMap[Method, InOut]

  private type InOut = MutableMap[Option[Type], OutBody]

  private type OutBody = MutableMap[Option[Type], Body[Any, Any]]

  private final val resourcemethods = new scala.collection.parallel.mutable.ParTrieMap[Class[_ <: Resource], Methods]

}

import javax.xml.bind.annotation.{ XmlAccessorType, XmlRootElement }
import javax.xml.bind.annotation._

@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.PROPERTY)
case class User(

  @xmlAttribute name: String,

  @xmlAttribute id: Int)

  extends XmlMarshaled

  with JsonMarshaled {

  def this() = this(null, -1)

}

class TestResource extends Resource {

  Post { in: String ⇒ in.reverse }

  Post { user: User ⇒ User(user.name + " Smith", user.id + 10) }

  Post { form: Map[String, String] ⇒ form.mkString("&") }

  Put { in: JObject ⇒ Json.parse("[1, 2, 3, " + Json.build(in) + "]") }

  Get { Json(List(request.query.getOrElse("no query").reverse)).asArray }

  Get { Json(context.variables).asObject }

  Get { "pong!".getBytes(text.ASCII) }

  Get { "pong!" } // need to lookup java.lang.String not just String, this is a known bug in 2.10

  Get { <a><b name="hello"/><c value="world">more</c></a> }

  Get { User("Joe", 1) } // what does the request accept with higher precedence?

  Get[JsonMarshaled] { User("Paul", 2) }

  Get[XmlMarshaled] { User("Bob", 3) }

  Get { form: Map[String, String] ⇒ form ++ Map("more" -> "values") }

  Head { response ++ Success.`206` }

}

object Test {

  def test = try { (new TestResource).handle(null, null) } catch { case e: Throwable ⇒ e.printStackTrace }

}
