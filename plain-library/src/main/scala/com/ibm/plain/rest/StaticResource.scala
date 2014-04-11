package com.ibm

package plain

package rest

import scala.collection.concurrent.TrieMap

import Resource.{ CachedMethod, MethodBody }
import http.{ Entity, Request }

/**
 *
 */
trait StaticResource

  extends Resource

  with StaticUniform {

  import Resource._

  @inline override protected[this] final def fromCache(request: Request): Option[CachedMethod] = requestmethods.get(request)

  @inline override protected[this] final def toCache(request: Request, cachedmethod: CachedMethod) = requestmethods.put(request, cachedmethod)

  private[this] final val requestmethods = new TrieMap[Request, (MethodBody, Any, Any ⇒ Option[Entity])]

}