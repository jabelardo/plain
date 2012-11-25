package com.ibm

package plain

package rest

package resource

import http._

final class PingResource

  extends Resource {

  Get { "pong!" }

  Get {
    response ++ Status.Success.`206`
    json.Json.parse("[1, 2, 3]").asArray
  } onComplete { response ⇒
    println("ping ok " + response + " " + context.##)
  } onFailure { e ⇒
    println("ping failed " + e + " " + context.##)
  }

}
