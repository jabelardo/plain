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

  headers: Map[String, String],

  entity: Option[Entity]) 
