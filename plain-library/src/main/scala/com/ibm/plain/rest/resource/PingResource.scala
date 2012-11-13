package com.ibm

package plain

package rest

package resource

import http.{ Request, Response }
import http.Status.Success

class PingResource

  extends Resource {

  def handle(request: Request, context: Context): Nothing = {
    completed(Response(Success.`200`), context)
    handled
  }

}
