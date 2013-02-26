package com.ibm

package plain

package concurrent

import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.dispatch.ExecutorServiceDelegate
import config.config2RichConfig
import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Concurrent

  extends BaseComponent[Concurrent]("plain-concurrent")

  with OnlyOnce {

  override def isStarted = null != actorSystem && !actorSystem.isTerminated

  override def isStopped = null != actorSystem && actorSystem.isTerminated

  override def start = {
    if (isEnabled) {
      if (isStopped) throw new IllegalStateException("Underlying system already terminated and cannot be started more than once.")
      createActorSystem
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
    createActorSystem
    val clazz = actorSystem.dispatcher.getClass
    val executorService = clazz.getDeclaredMethod("executorService")
    executorService.setAccessible(true)
    executorService.invoke(actorSystem.dispatcher).asInstanceOf[ExecutorServiceDelegate].executor
  }

  @inline private[this] final def createActorSystem = onlyonce {
    import config._
    import config.settings._
    actorSystem = ActorSystem(getString("plain.concurrent.actorsystem"), config.settings)
  }

  private[this] final var actorSystem: ActorSystem = null

}

/**
 * The Concurrent object.
 */
object Concurrent extends Concurrent
