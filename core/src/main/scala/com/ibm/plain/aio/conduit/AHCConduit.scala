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
import logging.Logger

/**
 *
 */
final class AHCConduit private (

  final val client: AsyncHttpClient,

  final val request: Request,

  final val contentLength: Long)

    extends AHCSourceConduit

    with AHCSinkConduit

    with TerminatingConduit {

  requestbuilder = client.prepareRequest(request)

}

/**
 *
 */
object AHCConduit {

  final def apply(client: AsyncHttpClient, request: Request): AHCConduit = apply(client, request, -1L)

  final def apply(client: AsyncHttpClient, request: Request, contentlength: Long) = new AHCConduit(client, request, contentlength)

}

/**
 * GET from the AHC component.
 */
sealed trait AHCSourceConduit

    extends AHCConduitBase

    with TerminatingSourceConduit {

  final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[A]) = {
    if (null != requestbuilder) {
      trace(s"first read : $requestbuilder")
      val r = requestbuilder
      requestbuilder = null
      result = r.execute(new AHCSourceHandler(buffer, handler, attachment))
    } else {
      trace("read")
    }
    await
  }

  protected[this] final class AHCSourceHandler[A](

    private[this] final val buffer: ByteBuffer,

    private[this] final val handler: Handler[A],

    private[this] final val attachment: A)

      extends AsyncCompletionHandler[Response] {

    final def onCompleted(innerresponse: Response): Response = {
      await
      handler.completed(0, attachment)
      result.done
      innerresponse
    }

    override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
      await
      buffer.put(part.getBodyByteBuffer)
      handler.completed(part.length, attachment)
      State.CONTINUE
    }

    override final def onThrowable(e: Throwable) = {
      error(s"onThrowable : $e")
      handler.failed(e, attachment)
    }

    override final def onContentWriteProgress(amount: Long, current: Long, total: Long) = {
      trace(s"onContentWriteProgress : amount = $amount, current = $current, total = $total")
      State.CONTINUE
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
      trace(s"first write : $requestbuilder")
      generator = new AHCBodyGenerator[A](buffer, handler, attachment)
      val r = requestbuilder
      r.setBody(generator)
      requestbuilder = null
      result = r.execute(new AsyncCompletionHandler[Response] {

        final def onCompleted(innerresponse: Response) = {
          trace(s"onCompleted : result = ${result.isDone} innerresponse = $innerresponse")
          result.done
          innerresponse
        }

        override final def onThrowable(e: Throwable) = {
          error(s"onThrowable : $e")
          handler.failed(e, attachment)
        }

        override final def onContentWriteProgress(amount: Long, current: Long, total: Long) = {
          trace(s"onContentWriteProgress : amount = $amount, current = $current, total = $total")
          State.CONTINUE
        }

      })
    } else {
      trace("write")
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

      def close: Unit = if (isopen.compareAndSet(true, false)) {
        handler.completed(-1, attachment)
      }

      def getContentLength = contentLength

      def read(buffer: java.nio.ByteBuffer): Long = {
        await
        val len = min(buffer.remaining, innerbuffer.remaining)
        buffer.put(innerbuffer.array, innerbuffer.position, len)
        innerbuffer.position(innerbuffer.position + len)
        spawn { handler.completed(len, attachment) }
        if (0 >= len) -1L else len
      }

    }

    private[this] final val isopen = new AtomicBoolean(true)

  }

  private[this] final var generator: AHCBodyGenerator[_] = null

}

/**
 *
 */
sealed trait AHCConduitBase

    extends Conduit

    with Logger {

  /**
   * Must not close AHCClient as it used by others in parallel.
   */
  def close = {
    trace("call to close()")
    isclosed = true
  }

  final def isOpen = !isclosed

  protected[this] val contentLength: Long

  /**
   * Call only after a call to transferAndWait, if the response is "almost there".
   */
  final def getResponse = {
    info(s"Calling getResponse.get")
    try {
      Some(result.get(defaulttimeout, TimeUnit.MILLISECONDS))
    } catch {
      case e: Throwable ⇒
        error(s"getResponse failed : $e")
        None
    }
  }

  protected[this] val client: AsyncHttpClient

  @volatile protected[this] final var requestbuilder: AsyncHttpClient#BoundRequestBuilder = null

  protected[this] final def await: Unit = {
    await(defaulttimeout)
  }

  @inline private[this] final def await(timeout: Long): Unit = {
    trace(s"cyclicbarrier await : timeout = $timeout")
    try {
      val index = cyclicbarrier.await(timeout, TimeUnit.MILLISECONDS)
      trace(s"cyclicbarrier await : index = $index")
    } catch {
      case e: Throwable ⇒
        error(s"cycnlicbarrier await : failed = $e")
        throw e
    }
  }

  protected[this] final val defaulttimeout = 90000

  protected[this] final val cyclicbarrier = new CyclicBarrier(2)

  @volatile protected[this] final var result: ListenableFuture[Response] = null

  @volatile private[this] final var isclosed = false

}
