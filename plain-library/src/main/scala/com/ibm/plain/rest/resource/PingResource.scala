package com.ibm

package plain

package rest

package resource

import http.{ Request, Response }
import http.Status.Success
import http.Entity._
import aio._

class PingResource

  extends Resource {

  override def handle(request: Request, context: Context): Nothing = {
    println(request)
    request.entity match {
      case Some(ContentEntity(length, contenttype)) if length < Int.MaxValue ⇒
        println(length)
        arr = new Array[Byte](length.toInt)
        transfer(context.io ++ length, arr, Adaptor(this, context))
      case _ ⇒ println("not handled")
    }
    completed(Response(Success.`200`), context)
  }

  override def completed(response: Response, context: Context) = {
    try println(new String(arr, text.ASCII)) catch { case e: Throwable ⇒ println(e) }
    println("response " + response)
    super.completed(response, context)
  }

  private[this] final var arr: Array[Byte] = null

}
