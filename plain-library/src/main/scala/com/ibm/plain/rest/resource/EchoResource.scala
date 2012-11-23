package com.ibm

package plain

package rest

package resource

import aio.FileByteChannel.forWriting
import aio.transfer
import http.Response

class EchoResource

  extends Resource {

  /**
   * The framework must add the `Content-length` or the deducted length from the transfer-decoding to the context.io.
   * Awkward that we need a (): Unit here at the end, it won't work with a Nothing.
   */
  // Post { transfer(context.io, forWriting("/tmp/bla1"), Adaptor(this, context)); () }

  /**
   * Should be onComplete directly attached to Post.
   */
  override def completed(response: Response, context: Context) = {
    println("response " + response)
    super.completed(response, context)
  }

}
