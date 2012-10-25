package com.ibm.plain
package lib

package rest

import java.nio.charset.Charset

import text.UTF8

import http.Status._
import http.Entity._
import http.{ Entity, Request, Status }

import Resource._

class PingResource

  extends BaseResource {

  override final def get = {
    println("finally we are here")
    Ok("PONG!")
  }
}
