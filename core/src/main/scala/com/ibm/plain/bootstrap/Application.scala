package com.ibm

package plain

package bootstrap

import scala.collection.mutable.ListBuffer
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
    if (!disableApplicationExtensions) createExtensions
    createExternals
    sortComponents
    components.filter(_.isEnabled).foreach(_.preStart)
    components.filter(_.isEnabled).foreach(_.doStart)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { def run = teardown }))
    runExtensions
  }

  /**
   * Initialize data in external components before starting the bootstrap process. See Camel for example.
   */
  private[this] final def createExternals = components.insertAll(components.size, subClasses(classOf[ExternalComponent[_]]).map(try _.newInstance catch {
    case e: Throwable ⇒ e.printStackTrace; null
  }).toSeq.filter(null != _))

  /**
   * Sort all components by their dependency order.
   */
  private[this] final def sortComponents = {

    final class Node( final val component: Component[_], dependencyclasses: List[Class[_ <: Component[_]]]) {

      override final def toString = component.name

      override final def equals(other: Any) = other match { case node: Node ⇒ component.name == node.component.name case _ ⇒ false }

      override final def hashCode = component.name.hashCode

      final def edges = components.filter(c ⇒ dependencyclasses.contains(c.getClass)).map(c ⇒ new Node(c, c.dependencies.toList)).toList

    }

    def recurse(node: Node, graph: ListBuffer[Node], seen: ListBuffer[Node]): Unit = {
      seen += node
      node.edges.foreach { e ⇒
        if (!graph.contains(e)) {
          if (seen.contains(e)) throw new RuntimeException("Circular dependency detected : " + node + " ⇒ " + e)
          recurse(e, graph, seen)
        }
      }
      graph += node
    }

    val seen = new ListBuffer[Node]
    val graph = new ListBuffer[Node]
    components.filter(_.isEnabled).foreach(c ⇒ recurse(new Node(c, c.dependencies.toList), graph, seen))
    val enabledcomponents = graph.map(_.component).toList.distinct
    enabledcomponents.foreach(_.enable)
    val disabledcomponents = components.filter(!_.isEnabled)
    components.clear
    components.insertAll(0, (enabledcomponents ++ disabledcomponents).map(_.asInstanceOf[BaseComponent[_]]))
  }

  private[this] final def createExtensions = extensions.insertAll(extensions.size, subClasses(classOf[ApplicationExtension]).map(try _.newInstance catch {
    case e: Throwable ⇒ e.printStackTrace; null
  }).toSeq.filter(null != _))

  final def teardown = onlyonce {
    try {
      val shutdown = new Thread(new Runnable { def run = { Thread.sleep(5000); terminateJvm(new RuntimeException("Forcing hard shutdown now."), -1, false) } })
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

  final def register(component: Component[_]): Application = synchronized { components += component.asInstanceOf[BaseComponent[_]]; this }

  final def unregister(component: Component[_]) = synchronized { components.remove(components.indexOf(component.asInstanceOf[BaseComponent[_]])); this }

  final def uptime = now - starttime

  final def getComponents(componentclass: Class[_ <: Component[_]]) = synchronized { components.filter(c ⇒ componentclass.isAssignableFrom(c.getClass)).clone.toList }

  private[this] final def runExtensions = extensions.foreach { extension ⇒
    try
      try extension.run
      catch { case NonFatal(e) ⇒ println(extension.getClass + " : " + e) }
    catch { case e: Throwable ⇒ terminateJvm(e, -1, false) }
  }

  private[this] final val components = new ListBuffer[BaseComponent[_]]

  private[this] final val extensions = new ListBuffer[ApplicationExtension]

  private[this] final val starttime = now

}

/**
 * The Application singleton.
 */
private[plain] object Application

  extends Singleton[Application](new Application)