package com.ibm.plain
package integration
package spaces

import java.net.URI
import java.nio.file.Paths

import scala.beans.BeanProperty

import org.apache.camel.{ Consumer, Endpoint, Exchange, Processor, Producer }
import org.apache.camel.impl.{ DefaultComponent, DefaultEndpoint, DefaultProducer }
import org.apache.camel.spi.{ UriEndpoint, UriParam }

import crypt.Uuid
import http.Method
import logging.Logger
import json.Json
import time.timeMillis

/**
 * Represents the component that manages {@link SpacesEndpoint}, Producer only (no from(spaces))
 *
 * Uri scheme: spaces:space[?parameters]
 *
 * Examples:
 *
 *  to("spaces://repository")
 *  to("spaces://repository?method=get")
 *  to("spaces:repository?method=get&purgeDirectory=true") *  to("spaces://repository?method=get") *  to("spaces://repository?method=get")
 *  to("spaces:repository?method=put")
 *
 * method: get | put | delete : default: get
 * purgeDirectory: true | false : default: false, only valid for method get, will empty the localDirectory if it exists
 *
 * space: a configured space on the server, if you prefer to specify the space as an option, this has preference over a given space in the path
 * useConduits: true | false : default: false; true: run everything with in sockets and conduits, false: use temporary files in between
 *
 * the IN message must contain the following headers (case-sensitive):
 * spaces.containerUuid : a 32 hex uuid string; this can be omitted for PUT, it will generate a uuid and set this variable on the message
 * spaces.localDirectory : a valid local filepath to a directory; it must exist on PUT, it will be created if necessary on GET and it can be omitted on DELETE
 *
 */

final class SpacesComponent

    extends DefaultComponent

    with Logger {

  protected final def createEndpoint(uri: String, remaining: String, parameters: java.util.Map[String, Object]): Endpoint = {
    val endpoint = new SpacesEndpoint(uri, this)
    setProperties(endpoint, parameters)
    endpoint
  }

}

/**
 * Represents a Spaces endpoint.
 */
@UriEndpoint(scheme = "spaces", title = "spaces", syntax = "spaces:path")
class SpacesEndpoint(

  uri: String,

  component: SpacesComponent)

    extends DefaultEndpoint(uri, component)

    with Logger {

  final def this() = this(null, null)

  final def this(uri: String) = this(uri, null)

  final def createProducer: Producer = {
    new SpacesProducer(this)
  }

  final def createConsumer(processor: Processor): Consumer = {
    throw new InstantiationException(s"createConsumer is not implemented for $this")
  }

  final def isSingleton = true

  /**
   * Parameters
   */

  final def getSpaceName = {
    if (null == space) {
      val u = URI.create(uri)
      space = u.getAuthority match { case null ⇒ u.getPath case a ⇒ a }
      require(null != space, s"Cannot deduct a valid space name from this uri : $u")
      space
    } else {
      space
    }
  }

  @BeanProperty @UriParam protected[this] final var space: String = null

  @BeanProperty @UriParam protected[this] final var method: String = "get"

  @BeanProperty @UriParam protected[this] final var purgeDirectory: Boolean = false

  @BeanProperty @UriParam protected[this] final var useConduits: Boolean = useConduitsDefault

}

/**
 * The Spaces producer.
 */
final class SpacesProducer(

  endpoint: SpacesEndpoint)

    extends DefaultProducer(endpoint)

    with Logger {

  final def process(exchange: Exchange): Unit = {
    val space = endpoint.getSpaceName
    val method = Method(endpoint.getMethod)
    val purgeDirectory = endpoint.getPurgeDirectory
    val useConduits = endpoint.getUseConduits
    method match {
      case Method.GET ⇒
        require(null != exchange.getIn.getHeader("spaces.localDirectory", classOf[String]), s"Cannot GET from a spaces container without spaces.localDirectory set as a message header. ")
        require(null != exchange.getIn.getHeader("spaces.containerUuid", classOf[String]), s"Cannot GET from a spaces container without spaces.containerUuid set as a message header.")
        val localdirectory = Paths.get(exchange.getIn.getHeader("spaces.localDirectory", classOf[String]))
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String])
        val (statuscode, ms) = timeMillis(SpacesClient.instance.get(space, containeruuid, localdirectory, purgeDirectory))
        info("GET " + containeruuid + " INTO " + localdirectory + " : " + statuscode + " (" + ms + " ms)")
        require(200 == statuscode, s"GET $containeruuid failed : statuscode = $statuscode")
      case Method.PUT ⇒
        require(null != exchange.getIn.getHeader("spaces.localDirectory", classOf[String]), s"Cannot PUT to a spaces container without spaces.localDirectory set as a message header. ")
        val localdirectory = Paths.get(exchange.getIn.getHeader("spaces.localDirectory", classOf[String]))
        require(localdirectory.toFile.exists, s"Cannot PUT to a spaces container from a non-existing directory : $localdirectory")
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String]) match {
          case null ⇒
            val uuid = Uuid.newUuid
            exchange.getIn.setHeader("spaces.containerUuid", uuid.toString)
            uuid
          case uuid ⇒
            Uuid.fromString(uuid)
        }
        val (statuscode, ms) = timeMillis(SpacesClient.instance.put(space, containeruuid, localdirectory))
        info("PUT " + containeruuid + " FROM " + localdirectory + " : " + statuscode + " (" + ms + " ms)")
        exchange.getIn.removeHeaders("spaces.localDirectory")
        require(201 == statuscode, s"PUT $containeruuid failed : statuscode = $statuscode")
      case Method.POST ⇒
        require(null != exchange.getIn.getHeader("spaces.localDirectory", classOf[String]), s"Cannot POST to a space without spaces.localDirectory set as a message header. ")
        require(null != exchange.getIn.getHeader("spaces.containerContent", classOf[String]), s"Cannot POST to a space without spaces.containerContent set as a message header. ")
        exchange.getIn.removeHeaders("spaces.containerUuid")
        val localdirectory = Paths.get(exchange.getIn.getHeader("spaces.localDirectory", classOf[String]))
        val containercontent = exchange.getIn.getHeader("spaces.containerContent", classOf[String])
        val content = ignoreOrElse(Json.parse(containercontent).asObject, Map.empty)
        val contentfilepath = exchange.getIn.getBody(classOf[String])
        val statuscode = if (0 < content.size) {
          val (statuscode, ms) = timeMillis(SpacesClient.instance.post(space, contentfilepath, localdirectory, purgeDirectory))
          info("POST " + contentfilepath + " INTO " + localdirectory + " : " + statuscode + " (" + ms + " ms)")
          statuscode
        } else {
          warn(s"Nothing to POST : $content")
          200
        }
        exchange.getIn.removeHeaders("spaces.containerContent")
        require(200 == statuscode, s"POST failed : statuscode = $statuscode")
      case Method.DELETE ⇒
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String])
        require(null != containeruuid, s"Cannot DELETE a spaces container without spaces.containerUuid set as a message header.")
        exchange.getIn.removeHeaders("spaces.containerUuid")
        unsupported
    }
  }

}
