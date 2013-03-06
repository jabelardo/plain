package com.ibm

package plain

package rest

package resource

import aio.FileByteChannel.forWriting
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

  //  Get { s: String ⇒ println(s); s }
  //
  //  Get { form: Form ⇒
  //    println("form " + form)
  //    response ++ http.Status.Success.`206`
  //    println(build(json.Json(form.values.flatten).asArray))
  //    val m = new Matching
  //    println(m.encodeForm(form))
  //    json.Json(form.values.flatten).asArray
  //  } onComplete { response ⇒
  //    println("ping ok " + response + " " + context.##)
  //  } onFailure { e ⇒
  //    println("ping failed " + e + " " + context.##)
  //  }
  //
  //  Post { user: User ⇒ println("we are in Post(User) : " + user); User(user.name + " Smith", user.id + 10) }
  //
  //  Post { json: Json ⇒ println("we are in Post(Json) : " + build(json)); build(json) }
  //
  //  Post { s: String ⇒ s.reverse }
  //
  //  Post { entity: Entity ⇒
  //    println("we are in Post(Entity) + entity");
  //    transfer(context.io, forWriting(if (os.isWindows) "nul" else "/dev/null"), Adaptor(this, context))
  //    ()
  //  } onComplete { response: Response ⇒
  //    println(response);
  //  } onFailure { e: Throwable ⇒
  //    println(e)
  //  }

}

object PingResource {

  val pong = {
    val s = new StringBuffer; (1 to 1000).foreach(s.append("ABCkltjaeslkjasdklfjasölkfjasdlkfj alöksfj aslkfj alksjf lakösj pong!").append(_).append("\n")); s.toString
    // "pong!".getBytes
  }

}
