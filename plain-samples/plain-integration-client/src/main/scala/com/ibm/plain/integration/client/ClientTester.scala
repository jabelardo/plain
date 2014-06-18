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
    val client = new AsyncHttpClient(
      new AsyncHttpClientConfig.Builder().
        setRequestTimeoutInMs(timeout).
        setConnectionTimeoutInMs(timeout).
        setIdleConnectionTimeoutInMs(timeout).
        setIdleConnectionInPoolTimeoutInMs(timeout).
        build)
    val request = new RequestBuilder("GET").
      setUrl(url).
      setHeader("Accept-Encoding", "gzip").
      build
    val source = GzipConduit(AHCConduit(client, request))
    val destination = TarConduit("/tmp/test.dir", true)
    var c = 0L
    var p = 0L
    val exchange = aio.client.ClientExchange(
      source, destination, null, i ⇒ { c += i; if (p < c - 10000) { p = c; println(c) } })
    infoMillis(exchange.transferAndWait)

    Application.instance.teardown
  }

}
