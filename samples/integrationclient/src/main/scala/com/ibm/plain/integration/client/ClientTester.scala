package com.ibm.plain
package integration
package client

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, HttpResponseBodyPart, RequestBuilder, Response }
import com.ning.http.client.AsyncCompletionHandler
import com.ning.http.client.AsyncHandler.{ STATE ⇒ State }
import com.ning.http.client.listener.{ TransferCompletionHandler, TransferListener }
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider
import com.ning.http.client.providers.apache.ApacheAsyncHttpProvider
import com.ning.http.client.providers.jdk.JDKAsyncHttpProvider

import aio.conduit.{ AHCConduit, FileConduit, DeflateConduit, GzipConduit, TarConduit }
import bootstrap.{ Application, ApplicationExtension }
import time.infoMillis

/**
 * Do some tests, then shutdown.
 */
final class ClientTester

    extends ApplicationExtension {

  final def run = try {

    //    import integration.spaces._
    //    SpacesClient.put(SpacesURI("myspace"), java.nio.file.FileSystems.getDefault.getPath("/tmp/bigtest"))

    val timeout = 60 * 60 * 1000
    val config = new AsyncHttpClientConfig.Builder().
      setRequestTimeoutInMs(timeout).
      setConnectionTimeoutInMs(timeout).
      setIdleConnectionTimeoutInMs(timeout).
      setIdleConnectionInPoolTimeoutInMs(timeout).
      build
    val client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config)
    for (i ← 1 to 1) {
      infoMillis {
        for (j ← 1 to 1) {
          val method = 1
          method match {
            case 1 ⇒
              val url = "http://harryklein.local:7070/spaces/myspace/375FA43D46984A0BB4989A0B70000000"
              val request = new RequestBuilder("PUT").
                setUrl(url).
                setBody(new File("/tmp/bigtest/all.tar.gz")).
                setHeader("Content-Encoding", "gzip").
                build
              val response = client.executeRequest(request)
              println(request)
              println(response.get)
            case 2 ⇒
              val url = "http://harryklein.local:7070/spaces/myspace/375FA43D46984A0BB4989A0B70000000"
              val request = new RequestBuilder("GET").
                setUrl(url).
                setHeader("Accept-Encoding", "gzip").
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
                      if (10000000 < total - p) { p = c; println(c + " " + part.isLast + " " + part.length + " " + total) }
                      State.CONTINUE
                    }
                    final def onCompleted(response: Response) = ()
                  }
                  val f = client.executeRequest(request, handler)
                  f.get(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
                  println("done")
                case 2 ⇒
                  val source = GzipConduit(AHCConduit(client, request))
                  val destination = TarConduit("/tmp/test.dir." + i + "." + j, true)
                  val exchange = aio.client.ClientExchange(source, destination)
                  exchange.transferAndWait
              }
              println(i * j)
          }
        }
      }
    }
    Thread.sleep(20000)
    client.close
  } finally {
    Thread.sleep(1000)
    Application.instance.teardown
  }

}
