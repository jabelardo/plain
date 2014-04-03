package com.ibm

package plain

package rest

import com.typesafe.config.Config

import scala.collection.concurrent.TrieMap

import aio.{ Exchange, ExchangeHandler }
import http.{ Entity, Request }

/**
 *
 */
trait StaticResource

  extends Resource

  with IsStatic {

  import Resource._

  @inline override protected[this] final def fromCache(request: Request): Option[CachedMethod] = requestmethods.get(request)

  @inline override protected[this] final def toCache(request: Request, cachedmethod: CachedMethod) = requestmethods.put(request, cachedmethod)

  private[this] final val requestmethods = new TrieMap[Request, (MethodBody, Any, Any ⇒ Option[Entity])]

}