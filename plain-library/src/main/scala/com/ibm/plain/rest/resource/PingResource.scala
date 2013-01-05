package com.ibm

package plain

package rest

package resource

import xml._
import json._
import json.Json._
import http.Entity

final class PingResource

  extends Resource {

  Get { "pong!" }

  Get { s: String ⇒ println(s); s }

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

  Post { json: Json ⇒ println("we are in Post(Json) : " + build(json)); build(json) }

  Post { s: String ⇒ s.reverse }

  Post { entity: Entity ⇒ println("we are in Post(Entity) + entity"); "pong!" }

}

