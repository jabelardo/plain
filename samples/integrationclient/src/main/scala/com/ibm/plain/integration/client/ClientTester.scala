package com.ibm.plain
package integration
package client

import java.io.{ File, FileInputStream, BufferedInputStream }
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, Body, BodyGenerator, HttpResponseBodyPart, RequestBuilder, Response }
import com.ning.http.client.AsyncCompletionHandler
import com.ning.http.client.AsyncHandler.{ STATE ⇒ State }
import com.ning.http.client.listener.{ TransferCompletionHandler, TransferListener }
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider
import com.ning.http.client.providers.apache.ApacheAsyncHttpProvider
import com.ning.http.client.providers.jdk.JDKAsyncHttpProvider
import com.ning.http.client.generators.{ InputStreamBodyGenerator }

import aio.conduit.{ AHCConduit, FileConduit, ChunkedConduit, DeflateConduit, GzipConduit, TarConduit }
import bootstrap.{ Application, ApplicationExtension }
import time.infoMillis
import concurrent.spawn

/**
 * Do some tests, then shutdown.
 */
final class ClientTester

    extends ApplicationExtension {

  final def run = try {

    val timeout = 15 * 1000
    val config = new AsyncHttpClientConfig.Builder().
      setRequestTimeoutInMs(timeout).
      setConnectionTimeoutInMs(timeout).
      setIdleConnectionTimeoutInMs(timeout).
      setIdleConnectionInPoolTimeoutInMs(timeout).
      build
    val client = new AsyncHttpClient(new NettyAsyncHttpProvider(config))
    for (i ← 1 to 1) {
      infoMillis {
        for (j ← 1 to 1) {
          val method = 1
          method match {
            case 1 ⇒
              val url = "http://harryklein.local:7070/spaces/myspace/375FA43D46984A0BB4989A0B70000000"
              val request = new RequestBuilder("PUT").
                setUrl(url).
                setHeader("Content-Encoding", "gzip").
                setHeader("Transfer-Encoding", "chunked").
                setHeader("Expect", "100-continue").
                build
              val source = TarConduit(new File("/tmp/bigtest2"))
              val destination = GzipConduit(ChunkedConduit(AHCConduit(client, request)))
              val exchange = aio.client.ClientExchange(source, destination)
              exchange.transferAndWait
            case 2 ⇒
              val bodygenerator = new BodyGenerator {

                final def createBody: Body = {
                  new MyBody
                }

                final class MyBody

                    extends Body {

                  final def getContentLength = { println("length -1"); -1L }

                  final def close = println("close")

                  final def read(buffer: ByteBuffer): Long = {
                    println("read " + buffer)
                    dumpStack
                    val l = if (first) {
                      first = false
                      buffer.put(bytes)
                      bytes.length
                    } else {
                      -1
                    }
                    println(l)
                    l
                  }

                  private[this] final var first = true

                  private[this] final val bytes = "this is a string.".getBytes

                }

              }
              val url = "http://harryklein.local:7070/spaces/myspace/375FA43D46984A0BB4989A0B70000000"
              val request = new RequestBuilder("PUT").
                setUrl(url).
                setBody(bodygenerator).
                // setBody(new InputStreamBodyGenerator(new FileInputStream("/tmp/bigtest/all.tar.gz"))).
                // setHeader("Content-Encoding", "gzip").
                build
              val response = client.executeRequest(request)
              println(request)
              println(response.get.getStatusCode)
            case 3 ⇒
              val url = "http://harryklein.local:7070/spaces/myspace/375FA43D46984A0BB4989A0B70000000"
              val request = new RequestBuilder("GET").
                setUrl(url).
                setHeader("Accept-Encoding", "deflate").
                build
              val choice = 2
              choice match {
                case 1 ⇒
                  val handler = new AsyncCompletionHandler[Unit] {
                    var c = 0
                    var p = 0
                    var total = 0L
                    override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
                      c += 1
                      total += part.length
                      if (100000 < total - p) { p = c; println(c + " " + part.isLast + " " + part.length + " " + total) }
                      State.CONTINUE
                    }
                    final def onCompleted(response: Response) = ()
                  }
                  val f = client.executeRequest(request, handler)
                  f.get(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
                case 2 ⇒
                  val source = DeflateConduit(AHCConduit(client, request))
                  val destination = TarConduit("/tmp/test.dir." + i + "." + j, true)
                  val exchange = aio.client.ClientExchange(source, destination)
                  exchange.transferAndWait
              }
              println(choice + " " + i + "." + j)
          }
        }
      }
    }
    Thread.sleep(2000)
    client.close
  } finally {
    Application.instance.teardown
  }

}
