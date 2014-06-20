package com.ibm

package plain

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
 *
 */
sealed trait AHCSourceConduit

  extends AHCConduitBase

  with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (null != requestbuilder) {
      requestbuilder.execute(new AHCSourceHandler(buffer, handler, attachment))
      requestbuilder = null
    }
    await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

    extends AsyncCompletionHandler[Unit] {

    final def onCompleted(response: Response) = ()

    var c = 0L

    override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
      await
      require(buffer.remaining >= part.length, buffer + " " + part.length)
      buffer.put(part.getBodyByteBuffer)
      try {
        handler.completed(part.length, attachment)
        State.CONTINUE
      } catch { case _: Throwable ⇒ State.ABORT }
    }

    override final def onThrowable(e: Throwable) = {
      handler.failed(e, attachment)
    }

  }

  private[this] final def await = ignore(cyclicbarrier.await)

  private[this] final val cyclicbarrier = new CyclicBarrier(2)

}

/**
 * Create a SinkConduit to write into a destination directory all content from the tar archive that is written to this sink.
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

  final def close = client.close

  final def isOpen = !client.isClosed

  protected[this] val client: AsyncHttpClient

  protected[this] final var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

}



