package com.ibm

package plain

package integration

package client

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import com.ning.http.client.{ AsyncHttpClient, HttpResponseBodyPart, RequestBuilder, Response }
import com.ning.http.client.AsyncCompletionHandler
import com.ning.http.client.AsyncHandler.{ STATE ⇒ State }
import com.ning.http.client.listener.{ TransferCompletionHandler, TransferListener }

import aio.conduit.{ AHCConduit, FileConduit, GzipConduit, TarConduit }
import bootstrap.{ Application, ApplicationExtension }

/**
 * Do some tests, then shutdown.
 */
final class ClientTester

    extends ApplicationExtension {

  final def run = {
    val client = new AsyncHttpClient
    val url = "http://127.0.0.1:7070/spaces/temporary/375FA43D46984A0BB4989A0B70000000"

    if (false) {

      val f = client.
        prepareGet(url).
        setHeader("Accept-Encoding", "deflate").
        execute(

          new AsyncCompletionHandler[Unit] {

            final def onCompleted(response: Response) = println("completed ")

            var c = 0
            var total = 0L

            override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
              c += 1
              total += part.length
              println(c + " " + part.isLast + " " + part.length + " " + total)
              println(part.getBodyByteBuffer)
              State.CONTINUE
            }

          })
      println("waiting")
      f.get
      println("done")

    } else if (false) {

      val t = new TransferCompletionHandler
      t.addTransferListener(new TransferListener {

        var c = 0

        def onBytesReceived(buffer: ByteBuffer) = {
          c += 1
          println("recv " + c + " " + aio.format(buffer))
          Thread.sleep(1000)
        }

        def onBytesSent(buffer: java.nio.ByteBuffer) = println("sent " + aio.format(buffer))

        def onRequestHeadersSent(h: com.ning.http.client.FluentCaseInsensitiveStringsMap) = println("sent " + h)

        def onRequestResponseCompleted() = println("completed")

        def onResponseHeadersReceived(h: com.ning.http.client.FluentCaseInsensitiveStringsMap) = println("received " + h)

        def onThrowable(e: Throwable) = println(e)

      })

      client.
        prepareGet(url).
        setHeader("Accept-Encoding", "gzip").
        execute(t)

    } else {

      println("test 3")
      val request = new RequestBuilder("GET").setUrl(url).setHeader("Accept-Encoding", "gzip").build
      val source = GzipConduit(AHCConduit(client, request))
      // val dest = FileConduit.forWriting("/tmp/test.bin")
      val dest = TarConduit(new File("/tmp/tardir"), true)
      object ClientExchange {

        val buffer = ByteBuffer.allocate(aio.defaultBufferSize)

        object ReadHandler

            extends Handler[Integer, Unit] {

          def failed(e: Throwable, a: Unit) = {
            println(e)
          }

          def completed(processed: Integer, a: Unit) = {
            println("r " + processed)
            if (0 < processed) {
              buffer.flip
              if (0 < buffer.remaining) dest.write(buffer, (), WriteHandler) else println("empty")
            }
          }
        }

        object WriteHandler

            extends Handler[Integer, Unit] {

          def failed(e: Throwable, a: Unit) = {
            println(e)
          }

          def completed(processed: Integer, a: Unit) = {
            println("w " + processed)
            if (0 > processed) {
              println("finished " + processed)
            } else {
              if (0 < buffer.remaining) {
                dest.write(buffer, (), this)
              } else {
                buffer.clear
                source.read(buffer, (), ReadHandler)
              }
            }
          }
        }

      }
      import ClientExchange._
      source.read(buffer, (), ReadHandler)
    }

    Thread.sleep(50000)
    client.close

    Application.instance.teardown
  }

}
