package com.ibm

package plain

package bootstrap

import scala.collection.mutable.{ ArrayBuffer, SynchronizedBuffer }
import scala.concurrent.duration.Duration

import time.now
import concurrent.OnlyOnce

/**
 *
 */
abstract sealed class Application

  extends OnlyOnce {

  override final def toString = components.toList.toString

  final def render: Array[String] = components.toArray.map(_.toString)

  final def bootstrap = {
    components.filter(_.isEnabled).foreach(_.doStart)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { def run = teardown }))
  }

  final def teardown = onlyonce { try { components.filter(_.isStarted).reverse.foreach { c ⇒ try c.doStop catch { case e: Throwable ⇒ println(c + " : " + e) } } } catch { case e: Throwable ⇒ terminateJvm(e, -1, false) } }

  final def awaitTermination(timeout: Duration) = components.filter(_.isStarted).foreach { c ⇒
    c.doAwaitTermination(timeout)
    teardown
  }

  final def register(component: Component[_]): Application = { components += component.asInstanceOf[BaseComponent[_]]; this }

  final def unregister(component: Component[_]) = { components -= component.asInstanceOf[BaseComponent[_]]; this }

  final def uptime = now - starttime

  final def getComponents(componentclass: Class[_ <: Component[_]]) = components.filter(_.getClass == componentclass)

  private[this] final val components = new ArrayBuffer[BaseComponent[_]] with SynchronizedBuffer[BaseComponent[_]]

  private[this] final val starttime = now

}

/**
 * The Application object.
 */
private[plain] object Application extends Application
