package com.ibm.plain
package aio
package conduit

import java.nio.ByteBuffer
import java.util.concurrent.CyclicBarrier

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
      r.execute(new AHCSourceHandler(buffer, handler, attachment))
    }
    await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

      extends AsyncCompletionHandler[Unit] {

    final def onCompleted(response: Response) = {
      await
      handler.completed(0, attachment)
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

  override def close = {
    println("ahc close")
    super.close
    await
  }

  var resp: ListenableFuture[Response] = null

  /**
   */
  final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (null != requestbuilder) {
      generator = new AHCBodyGenerator[A](buffer, handler, attachment)
      val r = requestbuilder
      r.setBody(generator)
      requestbuilder = null
      resp = r.execute(new AsyncCompletionHandler[Response] {

        final def onCompleted(response: Response) = {
          println("completed " + response.getStatusCode)
          response
        }

        override final def onStatusReceived(status: HttpResponseStatus) = {
          println("status " + status)
          super.onStatusReceived(status)
        }

        override final def onHeadersReceived(headers: HttpResponseHeaders) = {
          println("headers " + headers)
          super.onHeadersReceived(headers)
        }

      })
    } else {
      generator.asInstanceOf[AHCBodyGenerator[A]].handler = handler
    }
    println("write " + buffer)
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

      var total = 0L

      var isopen = new java.util.concurrent.atomic.AtomicBoolean(true)

      def close = if (isopen.compareAndSet(true, false)) {
        println("generator close " + isopen.get)
      }

      def getContentLength = -1L

      def read(buffer: java.nio.ByteBuffer): Long = try {
        println("read await " + isopen.get)
        if (isopen.get) {
          await
          println(format(innerbuffer, 100000))
          val len = min(buffer.remaining, innerbuffer.remaining)
          buffer.put(innerbuffer.array, innerbuffer.position, len)
          innerbuffer.position(innerbuffer.position + len)
          total += len
          println("len " + len + " total " + total)
          spawn { handler.completed(len, attachment) }
          if (0 == len) -1L else len
        } else {
          -1L
        }
      } catch { case e: Throwable ⇒ e.printStackTrace; throw e }

    }

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

  protected[this] val client: AsyncHttpClient

  protected[this] final var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

  protected[this] final def await = cyclicbarrier.await

  protected[this] final val cyclicbarrier = new CyclicBarrier(2)

  @volatile private[this] final var isclosed = false

}

