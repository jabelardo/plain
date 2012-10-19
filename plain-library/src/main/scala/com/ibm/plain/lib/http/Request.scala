package com.ibm.plain

package lib

package http

/**
 * The classic http request.
 */
case class Request(

  method: Method,

  path: List[String],

  query: Option[String],

  version: Version,

  headers: List[Header],

  body: Option[RequestBody])

