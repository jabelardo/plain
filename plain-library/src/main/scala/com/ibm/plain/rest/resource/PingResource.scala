package com.ibm

package plain

package rest

package resource

final class PingResource

  extends Resource {

  Get { "pong!" }

  Get { form: Map[String, String] ⇒
    response ++ http.Status.Success.`206`
    json.Json(form).asObject
  } onComplete { response ⇒
    println("ping ok " + response + " " + context.##)
  } onFailure { e ⇒
    println("ping failed " + e + " " + context.##)
  }

}
