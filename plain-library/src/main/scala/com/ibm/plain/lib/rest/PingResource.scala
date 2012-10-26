package com.ibm.plain

package lib

package rest

import Resource.Ok

class PingResource

  extends BaseResource {

  try {
    val a = Template("system/division/{division}/department/{department}")
    val b = Template("system/location/{location}")
    val c = Template("user/{user}")
    println(a)
    println(b)
    println(c)
    val t = Templates(a, b, c)
    println(t)
  } catch {
    case e: Throwable ⇒ e.printStackTrace
  }

  override final def get = Ok("PONG!")

}
