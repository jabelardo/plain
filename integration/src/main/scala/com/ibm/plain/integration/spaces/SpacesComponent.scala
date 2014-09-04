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

/**
 * Represents the component that manages {@link SpacesEndpoint}, Producer only (no from(spaces))
 *
 * Uri scheme: spaces:space?method
 *
 * Examples:
 *
 *  to("spaces://repository?method=get")
 *  to("spaces:repository?method=get")
 *  to("spaces:repository?method=put")
 *
 * method: get | put | delete            default: get
 * purgeDirectory: true | false          default: false, only valid for method get, will empty the localDirectory if it exists
 * space: a configured space on the server
 *
 * the in message must contain the following headers (case-sensitive):
 *
 * spaces.container-uuid : a 32 hex uuid string
 * spaces.localDirectory : a valid local filepath to a directory; it must exist on PUT, it may be created on GET and it can be omitted on DELETE
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
@UriEndpoint(scheme = "spaces")
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
    val purge = endpoint.getPurgeDirectory
    method match {
      case Method.GET ⇒
        val localdirectory = Paths.get(exchange.getIn.getHeader("spaces.localDirectory", classOf[String]))
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String])
        require(null != localdirectory, s"Cannot PUT to a spaces container without spaces.localDirectory set as a message header. ")
        require(null != containeruuid, s"Cannot GET from a spaces container without spaces.containerUuid set as a message header.")
        time.infoMillis("GET " + containeruuid)(SpacesClient.instance.get(space, containeruuid, localdirectory, purge))
        exchange.getIn.removeHeader("spaces.localDirectory")
        exchange.getIn.removeHeader("spaces.containerUuid")
      case Method.PUT ⇒
        val localdirectory = Paths.get(exchange.getIn.getHeader("spaces.localDirectory", classOf[String]))
        require(null != localdirectory, s"Cannot PUT to a spaces container without spaces.localDirectory set as a message header. ")
        require(localdirectory.toFile.exists, s"Cannot PUT to a spaces container from a non-existing directory : $localdirectory")
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String]) match {
          case null ⇒
            val uuid = Uuid.newUuid
            exchange.getIn.setHeader("spaces.containerUuid", uuid.toString)
            uuid
          case uuid ⇒
            Uuid.fromString(uuid)
        }
        time.infoMillis("PUT " + containeruuid)(SpacesClient.instance.put(space, containeruuid, localdirectory))
        exchange.getIn.removeHeader("spaces.localDirectory")
      case Method.DELETE ⇒
        val containeruuid = exchange.getIn.getHeader("spaces.containerUuid", classOf[String])
        require(null != containeruuid, s"Cannot DELETE a spaces container without spaces.containerUuid set as a message header.")
        exchange.getIn.removeHeader("spaces.containerUuid")
    }
  }

}
