package com.ibm

package plain

package rest

package resource

final class PingResource

  extends StaticResource {

  import PingResource._

  Get { pong }

  Get { _: String ⇒ pong }

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

}
