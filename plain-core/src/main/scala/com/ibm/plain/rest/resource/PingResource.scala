package com.ibm

package plain

package rest

package resource

/**
 *
 */
final class PingResource

    extends StaticResource {

  import PingResource._

  Get { pong }

  Get { _: String ⇒ pong }

}

/**
 *
 */
object PingResource {

  final val pong = "pong!".getBytes

}
