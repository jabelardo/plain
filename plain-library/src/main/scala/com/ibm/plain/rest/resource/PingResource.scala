package com.ibm

package plain

package rest

package resource

import xml._
import json._
import json.Json._

final class PingResource

  extends Resource {

  Get { "pong!" }

  Get { form: Map[String, String] ⇒
    println("form " + form)
    response ++ http.Status.Success.`206`
    println(build(json.Json(form).asObject))
    json.Json(form).asObject
  } onComplete { response ⇒
    println("ping ok " + response + " " + context.##)
  } onFailure { e ⇒
    println("ping failed " + e + " " + context.##)
  }

  Post { user: User ⇒ println("we are in Post(User) : " + user); User(user.name + " Smith", user.id + 10) }

  Post { json: Json ⇒ build(json) }

  Post { s: String ⇒ s.reverse }

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

