package com.ibm

package plain

package rest

package resource

import http._

final class PingResource

  extends Resource {

  // Get { request: Request ⇒ response: Response ⇒ context: Context ⇒ "pong!" }

  Get0 { 42 }

}
