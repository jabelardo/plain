package com.ibm.plain

package lib

package rest

import Resource.Ok

class PingResource

  extends BaseResource {

  try {
    val resourceclass = Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]]

    val a = Template("system/division/{division}/department/{department}", resourceclass)
    val b = Template("system/division/{division}/manager/{manager}", resourceclass)
    val c = Template("system/location/{location}", resourceclass)
    val d = Template("user/{user}", resourceclass)
    
    println(a)
    println(b)
    println(c)
    println(d)
    println(Templates(a, b, c, d))
  } catch {
    case e: Throwable ⇒ e.printStackTrace
  }

  override final def get = Ok("PONG!")

}
