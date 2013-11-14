package com.ibm

package plain

package servlet

import javax.{ servlet ⇒ js }
import plain.http.Status.ClientError.`404`

final case class RequestDispatcher(

  private final val path: String)

  extends js.RequestDispatcher {

  final def forward(request: js.ServletRequest, response: js.ServletResponse) = {

  }

  final def include(request: js.ServletRequest, response: js.ServletResponse) = {

  }

  private[this] final def inputstream(path: String, request: js.ServletRequest) = request.getServletContext.getClassLoader.getResourceAsStream(path) match {
    case null ⇒ throw `404`
    case in ⇒ in
  }

}
