package com.ibm.plain

package lib

package concurrent

import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.dispatch.ExecutorServiceDelegate
import config.config2RichConfig
import lib.bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Concurrent

  extends BaseComponent[Concurrent]("plain-concurrent") {

  override def isStopped = null != actorSystem && actorSystem.isTerminated

  override def start = {
    if (isEnabled) {
      if (isStopped) throw new IllegalStateException("Underlying system already terminated and cannot be started more than once.")
      if (null == actorSystem) actorSystem = {
        import config._
        import config.settings._
        ActorSystem(getString("plain.concurrent.actorsystem", "default"), config.settings)
      }
    }
    this
  }

  override def stop = {
    if (isStarted) actorSystem.shutdown
    this
  }

  override def awaitTermination(timeout: Duration) = if (!actorSystem.isTerminated) actorSystem.awaitTermination(timeout)

  final def dispatcher = actorSystem.dispatcher

  final def scheduler = actorSystem.scheduler

  /**
   * Provide access to the protected field 'executorService' of the dispatcher in order to share the nicely configured threadpool from akka.
   */
  final lazy val executor = {
    import language.existentials
    if (null == actorSystem) actorSystem = {
      import config._
      import config.settings._
      ActorSystem(getString("plain.concurrent.actorsystem", "default"), config.settings)
    }
    val clazz = actorSystem.dispatcher.getClass
    val executorService = clazz.getDeclaredMethod("executorService")
    executorService.setAccessible(true)
    executorService.invoke(actorSystem.dispatcher).asInstanceOf[ExecutorServiceDelegate].executor
  }

  @volatile private[this] var actorSystem: ActorSystem = null

}

/**
 * The Concurrent object.
 */
object Concurrent extends Concurrent

