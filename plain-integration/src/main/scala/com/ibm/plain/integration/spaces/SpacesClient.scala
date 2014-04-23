package com.ibm

package plain

package integration

package spaces

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

import logging.Logger

/**
 *
 */
final class SpacesClient private

  extends Logger {

  def doGet(uri: String) = {
    val client = HttpClients.createDefault
    try {
      val request = new HttpGet(uri)
      val handler = new ResponseHandler[String] {
        def handleResponse(response: HttpResponse): String = {
          response.getStatusLine.getStatusCode match {
            case 200 ⇒
              info(EntityUtils.toString(response.getEntity))
              response.getEntity().getContent
            case e ⇒ error("status code " + e)
          }
          null
        }
      }
      client.execute(request, handler)
    } finally client.close
  }

}

/**
 *
 */
object SpacesClient {

  final def get(uri: String) = new SpacesClient().doGet(uri)

}