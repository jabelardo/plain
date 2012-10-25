package com.ibm.plain

package lib

package rest

import http._

/**
 * The base class for all client rest dispatchers. The client rest dispatchers will be instantiated using their class name from the configuration via reflection.
 */
abstract class RestDispatcher

  extends HttpDispatcher {

}

/**
 * The default rest-dispatcher, it will always respond with 501.
 */
class DefaultRestDispatcher

  extends RestDispatcher {

  def dispatch(request: Request): Option[Response] = Some(Response(Status.ServerError.`501`))

}
