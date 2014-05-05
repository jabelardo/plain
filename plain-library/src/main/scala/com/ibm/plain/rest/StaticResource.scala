package com.ibm

package plain

package rest

import scala.collection.concurrent.TrieMap

import com.typesafe.config.Config

import Resource.{ CachedMethod, MethodBody }
import http.{ Entity, Request }

/**
 *
 */
trait StaticResource

  extends Resource

  with StaticUniform {

  import Resource._

  @inline def init(config: Config) = ()

  @inline override protected[this] final def fromCache(request: Request): Option[CachedMethod] = requestmethods.get(request.path)

  @inline override protected[this] final def toCache(request: Request, cachedmethod: CachedMethod) = requestmethods.put(request.path, cachedmethod)

  private[this] final val requestmethods = new TrieMap[Request.Path, (MethodBody, Any, Any ⇒ Option[Entity])]

}