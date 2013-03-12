package com.ibm

package plain

package rest

package resource

import scala.util.continuations.{ reset, suspendable }

import aio.ChannelTransfer
import aio.FileByteChannel.forWriting
import aio.FixedLengthChannel
import xml._
import json._
import json.Json._
import http.Entity._
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

  // Post { user: User ⇒ println("we are in Post(User) : " + user); User(user.name + " Smith", user.id + 10) }

  // Post { json: Json ⇒ println("we are in Post(Json) : " + build(json)); build(json) }

  // Post { s: String ⇒ s.reverse }

  Post { entity: Entity ⇒
    println("we are in Post(Entity) + entity " + entity)
    transfer(entity, forWriting("/tmp/test.txt"))
    "Thank you for this file.".getBytes
  }

}

object PingResource {

  val pong = {
    val s = new StringBuilder; (1 to 400).foreach(s.append("ABCkltjaeslkjasdklfjasölkfjasdlkfj alöksfj aslkfj alksjf lakösj pong!").append(_).append("\n")); s.toString
    "pong!".getBytes
  }

}

