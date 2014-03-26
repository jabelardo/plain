package com.ibm

package plain

package camel

import org.apache.camel.scala.dsl.builder.RouteBuilder

import scala.concurrent.duration.Duration
import scala.language.{ implicitConversions, postfixOps }

import akka.actor.{ Actor, ActorSystem, Props }
import akka.camel.{ Camel ⇒ AkkaCamel, CamelExtension, CamelMessage, Consumer, Producer, toActorRouteDefinition }
import akka.util.Timeout.intToTimeout
import bootstrap.ExternalComponent
import logging.Logger

/**
 *
 */
final class Camel

  extends ExternalComponent[Camel]("plain-camel")

  with Logger {

  override def order = bootstrapOrder

  override def start = {
    if (null == actorsystem && null == camel) {
      actorsystem = ActorSystem(actorSystemName, defaultExecutionContext = Some(concurrent.executor))
      camel = CamelExtension(actorsystem)
      camelextension = camel
      routes
    }
    this
  }

  override def stop = {
    if (null != actorsystem && null != camel) {
      actorsystem.shutdown
      actorsystem = null
      camel = null
      camelextension = null
      ignore(Thread.sleep(delayDuringShutdown))
    }
    this
  }

  lazy val routes = {

    //    val a = new Route {
    //
    //      from("file:/tmp/inbox?delete=true&delay=5000").
    //        convertBodyTo(classOf[String], "UTF-8").
    //        transform(body).
    //        to("file:/tmp/outbox")
    //
    //    }
    //
    val b = new Route {

      val myendpoint = actorsystem.actorOf(Props[MyEndpoint])
      camel.activationFutureFor(myendpoint)(actorInvocationTimeout, actorsystem.dispatcher)

      from("file:/tmp/inbox?noop=true&delay=5000").
        convertBodyTo(classOf[String], "UTF-8").
        to(myendpoint)
    }

    new Route {

      val myendpoint = actorsystem.actorOf(Props[MyEndpoint])

      from("servlet:/services/one?matchOnUriPrefix=true").
        convertBodyTo(classOf[String], "UTF-8").
        to(myendpoint)

      from("servlet:/services/two?matchOnUriPrefix=true").
        convertBodyTo(classOf[String], "UTF-8").
        to(myendpoint)

      from("direct:abc").to(myendpoint)

    }

    new RouteBuilder {

      "servlet:/services/three?matchOnUriPrefix=true" routeId "three" convertBodyTo (classOf[String], "UTF-8") to "direct:abc"

    }.addRoutesToCamelContext(camel.context)

  }

  override final def awaitTermination(timeout: Duration) = actorsystem.awaitTermination(timeout)

  private[this] final var actorsystem: ActorSystem = null

  private[this] final var camel: AkkaCamel = null

}

/**
 *
 */
class MyEndpoint extends Actor with Producer {

  def endpointUri = "file:/tmp/outbox"

  override def transformOutgoingMessage(msg: Any) = msg match {
    case msg: CamelMessage ⇒ msg.mapBody { body: String ⇒ if (null != body) body.toUpperCase else "empty" }
  }

}

