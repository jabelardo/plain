package com.ibm.plain

package lib

package rest

package resource

import concurrent.{ sleep, spawn }
import http.{ Request, Response }
import http.Status.Success

class PingResource

  extends BaseResource {

  override def handle(request: Request): Option[Response] = {
    println(request)
    println(variables + " " + remainder)
    spawn { try { sleep(2000); println(1 / 0); completed(Response(Success.`200`)) } catch { case e: Throwable ⇒ failed(e) } }
    None
  }

}
