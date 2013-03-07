package com.ibm

package plain

package rest

package resource

import aio.ChannelTransfer
import aio.FileByteChannel._
import xml._
import json._
import json.Json._
import http.{ Entity, Response }

final class PingResource

  extends Resource {

  import PingResource._

  Get { pong }

  Get { f: Form ⇒ json.Json(f) }

  Get { s: String ⇒ s.reverse }

  Get { user: User ⇒ User(user.name.reverse, user.id + 1) }

  Head {}

  Head { f: Form ⇒ }

  Post { user: User ⇒ println("we are in Post(User) : " + user); User(user.name + " Smith", user.id + 10) }

  Post { json: Json ⇒ println("we are in Post(Json) : " + build(json)); build(json) }

  Post { s: String ⇒ s.reverse }

  Post { entity: Entity ⇒
    println("we are in Post(Entity) + entity");
    ChannelTransfer(context.io.channel, forWriting(if (os.isWindows) "nul" else "/dev/null"), context.io).transfer
  } onComplete { response: Response ⇒
    println(response);
  } onFailure { e: Throwable ⇒
    println(e)
  }

}

object PingResource {

  val pong = {
    val s = new StringBuffer; (1 to 400).foreach(s.append("ABCkltjaeslkjasdklfjasölkfjasdlkfj alöksfj aslkfj alksjf lakösj pong!").append(_).append("\n")); s.toString
    // "pong!".getBytes
  }

}

