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

  override def toString = components.toList.toString

  def render: Array[String] = components.toArray.map(_.toString)

  def bootstrap = {
    components.filter(_.isEnabled).foreach(_.doStart)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { def run = teardown }))
  }

  def teardown = onlyonce { try { components.filter(_.isStarted).reverse.foreach { c ⇒ try c.doStop catch { case e: Throwable ⇒ println(c + " : " + e) } } } catch { case e: Throwable ⇒ terminateJvm(e, -1, false) } }

  def awaitTermination(timeout: Duration) = components.filter(_.isStarted).reverse.foreach { c ⇒
    c.doAwaitTermination(timeout)
    teardown
  }

  def register(component: Component[_]): Application = { components += component.asInstanceOf[BaseComponent[_]]; this }

  def unregister(component: Component[_]) = { components -= component.asInstanceOf[BaseComponent[_]]; this }

  def uptime = now - starttime

  private[this] final val components = new ArrayBuffer[BaseComponent[_]] with SynchronizedBuffer[BaseComponent[_]]

  private[this] final val starttime = now

}

/**
 * The Application object.
 */
private[plain] object Application extends Application
