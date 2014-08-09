package com.ibm.plain
package aio
package conduit

import java.nio.ByteBuffer
import java.util.concurrent.CyclicBarrier

import com.ning.http.client._
import com.ning.http.client.AsyncHttpClient._
import com.ning.http.client.AsyncHandler.{ STATE ⇒ State }
import com.ning.http.client.listener._

/**
 *
 */
final class AHCConduit private (

  protected[this] final val client: AsyncHttpClient,

  private[this] final val request: Request)

    extends AHCSourceConduit

    with AHCSinkConduit

    with TerminatingConduit {

  requestbuilder = client.prepareRequest(request)

}

/**
 *
 */
object AHCConduit {

  final def apply(client: AsyncHttpClient, request: Request) = new AHCConduit(client, request)

}

/**
 * GET from the AHC component.
 */
sealed trait AHCSourceConduit

    extends AHCConduitBase

    with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (null != requestbuilder) {
      val r = requestbuilder
      requestbuilder = null
      r.execute(new AHCSourceHandler(buffer, handler, attachment))
    }
    await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

      extends AsyncCompletionHandler[Unit] {

    final def onCompleted(response: Response) = ()

    override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
      await
      buffer.put(part.getBodyByteBuffer)
      handler.completed(part.length, attachment)
      State.CONTINUE
    }

    override final def onThrowable(e: Throwable) = {
      handler.failed(e, attachment)
    }

  }

  private[this] final def await = cyclicbarrier.await

  private[this] final val cyclicbarrier = new CyclicBarrier(2)

}

/**
 * PUT to the AHC component.
 */
sealed trait AHCSinkConduit

    extends AHCConduitBase

    with TerminatingSinkConduit {

  /**
   * Not yet implemented.
   */
  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = unsupported

}

/**
 *
 */
sealed trait AHCConduitBase

    extends Conduit {

  /**
   * Must not close client as it used by others in parallel.
   */
  final def close = ()

  final def isOpen = !client.isClosed

  protected[this] val client: AsyncHttpClient

  protected[this] final var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

}

