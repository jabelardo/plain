package com.ibm

package plain

package rest

package resource

import scala.reflect.runtime.universe

import rest.Resource

class PingResource

  extends Resource {

  Get { "pong!" }

}
