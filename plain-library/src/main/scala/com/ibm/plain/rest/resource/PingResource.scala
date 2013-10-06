package com.ibm

package plain

package rest

package resource

import json.Json

final class PingResource

  extends Resource {

  import PingResource._

  Get { pong }

  //  Get { Json(Map("Hello" -> "world!")) }

  //  Get { f: Form ⇒ json.Json(f) }
  //
  //  Get { s: String ⇒ s.reverse }
  //
  //  Get { user: User ⇒ User(user.name.reverse, user.id + 1) }
  //
  //  Head {}
  //
  //  Head { f: Form ⇒ }
  //
  //  Post { user: User ⇒ println("we are in Post(User) : " + user); User(user.name + " Smith", user.id + 10) }
  //
  //  Post { json: Json ⇒ println("we are in Post(Json) : " + build(json)); build(json) }
  //
  //  Post { s: String ⇒ s.reverse }
  //
  //  Post { entity: Entity ⇒
  //    val filename = request.query.get
  //    println("we are in Post(Entity) + entity " + entity + " " + entity.length + " " + filename)
  //    transfer(entity, forWriting(filename, entity.length))
  //    "Thank you for this file, we stored it under " + filename
  //  }

}

object PingResource {

  final val pong = "pong!".getBytes

  // final val pong = { val s = new StringBuilder; for (i ← 1 to 400) s.append("pong!"); s.toString.getBytes }

}
