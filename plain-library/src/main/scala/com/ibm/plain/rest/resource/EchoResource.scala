package com.ibm

package plain

package rest

package resource

import scala.reflect.runtime.universe.TypeTag.{ Any, Nothing }

import aio.FileByteChannel.forWriting
import aio.transfer
import http.Response
import rest.{ Context, Resource }

class EchoResource

  extends Resource {

  /**
   * plain must add the `Content-length` to the context.io. Awkward that we need a (): Unit here at the end, it won't work with a Nothing.
   */
  Post { transfer(context.io, forWriting("/tmp/bla1"), Adaptor(this, context)); () }

  override def completed(response: Response, context: Context) = {
    println("response " + response)
    super.completed(response, context)
  }

}
