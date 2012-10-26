package com.ibm.plain

package lib

package rest

import Resource.Ok

class PingResource

  extends BaseResource {

  override final def get = Ok("PONG!")

}
