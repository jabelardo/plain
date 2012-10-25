package com.ibm.plain.sample.helloworld

import com.ibm.plain.lib.http._
import com.ibm.plain.lib.rest.RestDispatcher

final class Dispatcher

  extends RestDispatcher {

  override def dispatch(request: Request): Option[Response] = {
    Some(Response(Status.ClientError.`404`))
  }

}
