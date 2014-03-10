package com.ibm

package plain

package camel

import org.apache.camel.builder.RouteBuilder

/**
 *
 */
trait Route

  extends RouteBuilder

  with DelayedInit {

  final def delayedInit(body: ⇒ Unit) = {
    body
    camelextension.context.addRoutes(this)
  }

  final def configure = ()

}

