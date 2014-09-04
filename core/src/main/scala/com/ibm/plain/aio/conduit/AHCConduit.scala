package com.ibm.plain
package aio
package conduit

import java.nio.ByteBuffer
import java.util.concurrent.{ CyclicBarrier, TimeUnit }
import java.util.concurrent.atomic.AtomicBoolean

import scala.math.min

import com.ning.http.client._
import com.ning.http.client.AsyncHttpClient._
import com.ning.http.client.AsyncHandler.{ STATE ⇒ State }
import com.ning.http.client.listener._

import concurrent.spawn

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
      response = r.execute(new AHCSourceHandler(buffer, handler, attachment))
    }
    await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

      extends AsyncCompletionHandler[Response] {

    final def onCompleted(innerresponse: Response): Response = {
      await(500)
      handler.completed(0, attachment)
      response.done
      innerresponse
    }

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

}

/**
 * PUT to the AHC component.
 */
sealed trait AHCSinkConduit

    extends AHCConduitBase

    with TerminatingSinkConduit {

  /**
   */
  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (null != requestbuilder) {
      generator = new AHCBodyGenerator[A](buffer, handler, attachment)
      val r = requestbuilder
      r.setBody(generator)
      requestbuilder = null
      response = r.execute(new AsyncCompletionHandler[Response] {

        final def onCompleted(innerresponse: Response) = {
          await(500)
          handler.completed(0, attachment)
          response.done
          innerresponse
        }

      })
    } else {
      generator.asInstanceOf[AHCBodyGenerator[A]].handler = handler
    }
    await
  }

  protected[this] final class AHCBodyGenerator[A](

    innerbuffer: ByteBuffer,

    var handler: Handler[A],

    attachment: A)

      extends BodyGenerator {

    final def createBody: Body = new AHCBody

    final class AHCBody

        extends Body {

      def close: Unit = isopen.compareAndSet(true, false)

      def getContentLength = -1L

      def read(buffer: java.nio.ByteBuffer): Long = try {
        await
        val len = min(buffer.remaining, innerbuffer.remaining)
        buffer.put(innerbuffer.array, innerbuffer.position, len)
        innerbuffer.position(innerbuffer.position + len)
        spawn { handler.completed(len, attachment) }
        if (0 == len) -1L else len
      } catch { case e: Throwable ⇒ e.printStackTrace; throw e }

    }

    private[this] final val isopen = new AtomicBoolean(true)

  }

  private[this] final var generator: AHCBodyGenerator[_] = null

}

/**
 *
 */
sealed trait AHCConduitBase

    extends Conduit {

  /**
   * Must not close AHCClient as it used by others in parallel.
   */
  def close = isclosed = true

  final def isOpen = !isclosed

  /**
   * Call only after a call to transferAndWait, if the response is "almost there".
   */
  final def getResponse = ignoreOrElse(Some(response.get(200, TimeUnit.MILLISECONDS)), None)

  protected[this] val client: AsyncHttpClient

  protected[this] final var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

  protected[this] final def await = cyclicbarrier.await

  protected[this] final def await(timeout: Long) = ignore(cyclicbarrier.await(timeout, TimeUnit.MILLISECONDS))

  protected[this] final val cyclicbarrier = new CyclicBarrier(2)

  protected[this] final var response: ListenableFuture[Response] = null

  @volatile private[this] final var isclosed = false

}

