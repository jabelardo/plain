package com.ibm.plain

package lib

package rest

package resource

import com.ibm.plain.lib.rest.{ Context, Resource }

import aio.FileByteChannel.forWriting
import aio.transfer
import http.{ Request, Response }
import http.Entity.ContentEntity
import http.Method.POST
import http.Status.ClientError

class EchoResource

  extends Resource {

  def handle(request: Request, context: Context): Nothing = request.method match {
    case POST ⇒ request.entity match {
      case Some(ContentEntity(length, _)) ⇒
        transfer(context.io ++ length, forWriting("/tmp/bla1"), Adaptor(this, context))
      case _ ⇒ throw ClientError.`400`
    }
    case _ ⇒ throw ClientError.`405`
  }

  override def completed(response: Response, context: Context) = {
    println("response " + response)
    super.completed(response, context)
  }

}
