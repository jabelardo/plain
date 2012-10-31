package com.ibm.plain

package lib

package rest

package resource

import concurrent.{ sleep, spawn }
import http.{ Request, Response }
import http.Status.Success

class PingResource

  extends Resource {

  def handle(request: Request, context: Context): Nothing = {
    println(request)
    println(context)
    // spawn { try { completed(Response(Success.`200`)) } catch { case e: Throwable ⇒ failed(e) } }
    completed(Response(Success.`200`), context)
    handled
  }

}
