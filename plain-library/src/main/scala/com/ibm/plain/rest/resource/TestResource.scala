package com.ibm

package plain

package rest

package resource

import scala.reflect._
import scala.reflect.runtime.universe._

import json._
import json.Json._
import xml._
import http._
import http.Request.Variables
import http.Status._
import http.Method._
import rest.Resource

class TestResource extends Resource {

  Post { in: String ⇒ in.reverse }

  Post { in: Array[Byte] ⇒ in.reverse }

  Post { user: User ⇒ User(user.name + " Smith", user.id + 10) }

  Post { form: Map[String, String] ⇒ form.mkString("&") }

  Put { in: JObject ⇒ Json.parse("[1, 2, 3, " + Json.build(in) + "]") }

  Get[JArray] { println(request); Json(List(request.query.getOrElse("no query").reverse)).asArray }

  Get[JObject] { Json(context.variables).asObject }

  Get { "pong!".getBytes(text.`US-ASCII`) }

  Get { "pong!" } // need to lookup with java.lang.String because Predef.String is simply a type alias and they don't work here.

  Get { <a><b name="hello"/><c value="world">more</c></a> }

  Get { User("Joe", 1) } // what does the request accept with higher precedence?

  Get[JsonMarshaled] { User("Paul", 2) }

  Get[XmlMarshaled] { User("Bob", 3) }

  Get { form: Map[String, String] ⇒ form ++ Map("more" -> "values") }

  Head { response ++ Success.`206` }

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

