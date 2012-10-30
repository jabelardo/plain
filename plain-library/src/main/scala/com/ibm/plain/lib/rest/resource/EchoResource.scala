package com.ibm.plain

package lib

package rest

package resource

import java.nio.channels.AsynchronousFileChannel
import java.nio.file.{ Paths, StandardOpenOption }

import com.ibm.plain.lib.rest.BaseResource

import aio.{ AsynchronousFileByteChannel, AsynchronousWriteChannel, transfer }
import http.{ Request, Response }
import http.Entity.RequestEntity
import http.Method.POST
import http.Status.{ ClientError, Success }

class EchoResource

  extends BaseResource {

  override def handle(request: Request): Option[Response] = request.method match {
    case POST ⇒ request.entity match {
      case Some(RequestEntity(read)) ⇒
        println("echo")
        val file = AsynchronousFileChannel.open(Paths.get("/tmp/bla1"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        transfer.apply(
          read,
          AsynchronousWriteChannel(AsynchronousFileByteChannel(file)),
          null,
          null,
          io.buffer)
        Thread.sleep(5000)
        Some(Response(Success.`200`))
      case _ ⇒ throw ClientError.`400`
    }
    case _ ⇒ throw ClientError.`405`
  }

}
