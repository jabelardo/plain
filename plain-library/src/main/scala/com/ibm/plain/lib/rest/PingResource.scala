package com.ibm.plain

package lib

package rest

import Resource.Ok

class PingResource

  extends BaseResource {

  try {
    val resourceclass = Class.forName("com.ibm.plain.lib.rest.PingResource").asInstanceOf[Class[Resource]]

    val f = Template("system/division/{division}", resourceclass)
    val a = Template("system/division/{division}/department/{department}", resourceclass)
    val b = Template("system/division/{division}/manager/{manager}", resourceclass)
    val g = Template("system/division/{division}/manager/{manager}/{salery}", resourceclass)
    val c = Template("system/location/{location}", resourceclass)
    val d = Template("user/{user}", resourceclass)
    val e = Template("ping", resourceclass)

    //    println(f)
    //    println(a)
    //    println(b)
    //    println(c)
    //    println(d)
    //    println(e)

    require(Templates(a, b, c, d, e, f, g).toString == Templates(g, f, e, d, c, b, a).toString)
    require(Templates(a, c, e, d, f, g, b).toString == Templates(g, e, d, f, c, b, a).toString)
    println(Templates(g, e, d, f, c, b, a))

  } catch {
    case e: Throwable ⇒ e.printStackTrace
  }

  override final def get = Ok("PONG!")

}
