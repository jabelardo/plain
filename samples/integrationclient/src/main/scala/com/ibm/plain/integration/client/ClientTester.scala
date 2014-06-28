package com.ibm

package plain

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

import aio.conduit.{ AHCConduit, FileConduit, DeflateConduit, GzipConduit, TarConduit }
import bootstrap.{ Application, ApplicationExtension }
import time.infoMillis

/**
 * Do some tests, then shutdown.
 */
final class ClientTester

    extends ApplicationExtension {

  final def run = {
    val url = "http://harryklein.local:7070/spaces/temporary/375FA43D46984A0BB4989A0B70000000"
    val timeout = 60 * 60 * 1000
    val config = new AsyncHttpClientConfig.Builder().
      setRequestTimeoutInMs(timeout).
      setConnectionTimeoutInMs(timeout).
      setIdleConnectionTimeoutInMs(timeout).
      setIdleConnectionInPoolTimeoutInMs(timeout).
      build
    val client = new AsyncHttpClient(new NettyAsyncHttpProvider(config), config)
    val request = new RequestBuilder("GET").
      setUrl(url).
      setHeader("Accept-Encoding", "gzip").
      build
    val choise = 2
    choise match {
      case 1 ⇒
        val handler = new AsyncCompletionHandler[Unit] {
          var c = 0
          var p = 0
          var total = 0L
          override final def onBodyPartReceived(part: HttpResponseBodyPart): State = {
            c += 1
            total += part.length
            if (1000000 < total - p) { p = c; println(c + " " + part.isLast + " " + part.length + " " + total) }
            State.CONTINUE
          }
          final def onCompleted(response: Response) = ()
        }
        val f = client.executeRequest(request, handler)
        f.get
        println("done")
        client.close
      case 2 ⇒
        val source = GzipConduit(AHCConduit(client, request))
        // val destination = FileConduit.forWriting("/dev/null")
        val destination = TarConduit("/tmp/test.dir", true)
        var c = 0L
        var p = 0L
        val exchange = aio.client.ClientExchange(source, destination)
        infoMillis(exchange.transferAndWait)
    }
    Application.instance.teardown
  }

}
