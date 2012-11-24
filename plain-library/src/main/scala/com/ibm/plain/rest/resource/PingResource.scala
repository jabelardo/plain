package com.ibm

package plain

package rest

package resource

import http._

final class PingResource

  extends Resource {

  Get { require(1 == request.path.length); "pong!" }

}
