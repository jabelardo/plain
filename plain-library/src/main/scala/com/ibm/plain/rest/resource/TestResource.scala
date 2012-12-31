//package com.ibm
//
//package plain
//
//package rest
//
//package resource
//
//import scala.reflect._
//import scala.reflect.runtime.universe._
//
//import json._
//import json.Json._
//import xml._
//import http._
//import http.Request.Variables
//import http.Status._
//import http.Method._
//import rest.Resource
//
//class TestResource extends Resource {
//
//  Post { in: String ⇒ in.reverse }
//
//  Post { user: User ⇒ User(user.name + " Smith", user.id + 10) }
//
//  Post { form: Map[String, String] ⇒ form.mkString("&") }
//
//  Put { in: JObject ⇒ Json.parse("[1, 2, 3, " + Json.build(in) + "]") }
//
//  Get[JArray] { println(request); Json(List(request.query.getOrElse("no query").reverse)).asArray }
//
//  Get[JObject] { Json(context.variables).asObject }
//
//  Get { "pong!".getBytes(text.`US-ASCII`) }
//
//  Get { "pong!" } // need to lookup with java.lang.String because Predef.String is simply a type alias and they don't work here.
//
//  Get { <a><b name="hello"/><c value="world">more</c></a> }
//
//  Get { User("Joe", 1) } // what does the request accept with higher precedence?
//
//  Get[JsonMarshaled] { User("Paul", 2) }
//
//  Get[XmlMarshaled] { User("Bob", 3) }
//
//  Get { form: Map[String, String] ⇒ form ++ Map("more" -> "values") }
//
//  Head { response ++ Success.`206` }
//
//  /**
//   * just for testing
//   */
//  override def test = {
//    println(request)
//    println(context)
//    println(m)
//    println(m.get(POST).get.get(Some(typeOf[String])).get.get(Some(typeOf[String])).get.body("this is the input"))
//    println(m.get(POST).get.get(Some(typeOf[User])).get.get(Some(typeOf[User])).get.body(User("Mary", 7)))
//    println(m.get(POST).get.get(Some(typeOf[Map[String, String]])).get.get(Some(typeOf[String])).get.body(Map("a" -> "x", "b" -> "y")))
//    println(m.get(PUT).get.get(Some(typeOf[JObject])).get.get(Some(typeOf[Json])).get.body(Json.parse("{\"name\":\"value\"}").asObject))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[scala.xml.Elem])).get.body(()))
//    println(new String(m.get(GET).get.get(None).get.get(Some(typeOf[Array[Byte]])).get.body(()).asInstanceOf[Array[Byte]]))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[java.lang.String])).get.body(()))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[User])).get.body(()))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[JsonMarshaled])).get.body(()).asInstanceOf[JsonMarshaled].toJson)
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[XmlMarshaled])).get.body(()).asInstanceOf[XmlMarshaled].toXml)
//    println(m.get(GET).get.get(Some(typeOf[Map[String, String]])).get.get(Some(typeOf[scala.collection.immutable.Map[String, String]])).get.body(Map("a" -> "x", "b" -> "y")))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[JObject])).get.body(()))
//    println(m.get(GET).get.get(None).get.get(Some(typeOf[JArray])).get.body(()))
//  }
//}
//
//object Test {
//
//  def test = {
//    val request = Request(GET, List("test"), Some("this is the query"), Version.`HTTP/1.1`, Map.empty, None)
//    val context = Context(null.asInstanceOf[aio.Io]) ++ Seq("remainder") ++ Map("user" -> "name")
//    try { (new TestResource).handle(request, context) } catch { case e: Throwable ⇒ println(e) }
//  }
//
//}
//
//import javax.xml.bind.annotation.{ XmlAccessorType, XmlRootElement }
//import javax.xml.bind.annotation._
//
//@XmlRootElement(name = "user")
//@XmlAccessorType(XmlAccessType.PROPERTY)
//case class User(
//
//  @xmlAttribute name: String,
//
//  @xmlAttribute id: Int)
//
//  extends XmlMarshaled
//
//  with JsonMarshaled {
//
//  def this() = this(null, -1)
//
//}
//
