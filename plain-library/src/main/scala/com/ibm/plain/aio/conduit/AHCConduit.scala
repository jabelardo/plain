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
    cyclicbarrier.await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

    extends AsyncCompletionHandler[Unit] {

    final def onCompleted(response: Response) = {
      cyclicbarrier.await
      handler.completed(-1, attachment)
    }

    override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
      cyclicbarrier.await
      if (0 >= part.length) println("part " + part.length)
      if (buffer.remaining < part.length) println("buffer to small " + buffer.remaining)
      buffer.put(part.getBodyByteBuffer)
      handler.completed(part.length, attachment)
      State.CONTINUE
    }

    override final def onThrowable(e: Throwable) = {
      handler.failed(e, attachment)
    }

  }

  private[this] final val cyclicbarrier = new CyclicBarrier(2)

}

/**
 * Create a SinkConduit to write into a destination directory all content from the tar archive that is written to this sink.
 */
sealed trait AHCSinkConduit

  extends AHCConduitBase

  with TerminatingSinkConduit {

  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
  }

}

/**
 * d
 *
 */
sealed trait AHCConduitBase

  extends Conduit {

  def close = client.close

  def isOpen = !client.isClosed

  protected[this] val client: AsyncHttpClient

  protected[this] var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

}



