package com.ibm

package plain

package bootstrap

import scala.collection.JavaConversions.{ collectionAsScalaIterable, seqAsJavaList }
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import concurrent.OnlyOnce
import reflect.subClasses
import time.now

/**
 *
 */
abstract sealed class Application

  extends OnlyOnce {

  override final def toString = components.toList.toString

  final def render: Array[String] = components.toArray.map(_.toString)

  final def bootstrap = {
    createExternals
    components.filter(_.isEnabled).foreach(_.doStart)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { def run = teardown }))
  }

  /**
   * Initialize data in external components before starting the bootstrap process. See Camel for example.
   */
  final def createExternals = {
    components.addAll(subClasses(classOf[ExternalComponent[_]]).map(try _.newInstance catch {
      case e: Throwable ⇒ e.printStackTrace; null
    }).toSeq.filter(null != _).sortWith { case (a, b) ⇒ a.order < b.order })
  }

  final def teardown = onlyonce {
    try {
      val shutdown = new Thread(new Runnable { def run = { Thread.sleep(15000); terminateJvm(new RuntimeException("Forcing hard shutdown now."), -1, false) } })
      shutdown.setDaemon(true)
      shutdown.start
      components.filter(_.isStarted).toList.reverse.foreach { c ⇒
        try c.doStop catch { case NonFatal(e) ⇒ println(c + " : " + e) }
      }
    } catch { case e: Throwable ⇒ terminateJvm(e, -1, false) }
  }

  final def awaitTermination(timeout: Duration) = components.filter(_.isStarted).foreach { c ⇒
    c.doAwaitTermination(timeout)
    teardown
  }

  final def register(component: Component[_]): Application = { components.add(component.asInstanceOf[BaseComponent[_]]); this }

  final def unregister(component: Component[_]) = { components.remove(component.asInstanceOf[BaseComponent[_]]); this }

  final def uptime = now - starttime

  final def getComponents(componentclass: Class[_ <: Component[_]]) = components.filter(_.getClass == componentclass)

  private[this] final val components = new java.util.concurrent.ArrayBlockingQueue[BaseComponent[_]](128)

  private[this] final val starttime = now

}

/**
 * The Application object.
 */
private[plain] object Application extends Application
