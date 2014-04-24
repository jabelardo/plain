package com.ibm

package plain

package bootstrap

import java.util.concurrent.LinkedBlockingQueue

import scala.collection.JavaConversions.{ collectionAsScalaIterable, seqAsJavaList }
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import concurrent.OnlyOnce
import reflect.subClasses
import time.now

/**
 *
 */
final class Application private

  extends OnlyOnce

  with IsSingleton {

  override final def toString = components.toList.toString

  final def render: Array[String] = components.toArray.map(_.toString)

  final def bootstrap = {
    createExternals
    enableNecessaryButDisabled
    components.foreach(println)
    components.filter(_.isEnabled).foreach(_.preStart)
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

  /**
   * Scan all components that other enabled components depend on and enable them in case they are disabled.
   */
  final def enableNecessaryButDisabled = {
    def recurse(dependencies: List[Class[_ <: Component[_]]]): Unit = dependencies match {
      case head :: tail ⇒
        components.filter(_.getClass == head).foreach { c ⇒
          c.enable
          recurse(c.dependencies.toList)
        }
        recurse(tail)
      case Nil ⇒
    }
    components.filter(!_.isEnabled).foreach(c ⇒ recurse(c.dependencies.toList))
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

  private[this] final val components = new LinkedBlockingQueue[BaseComponent[_]]

  private[this] final val starttime = now

}

/**
 * The Application singleton.
 */
private[plain] object Application

  extends Singleton[Application](new Application)