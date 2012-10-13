package com.ibm.plain

package lib

package concurrent

import scala.concurrent.util.Duration

import com.ibm.plain.lib.bootstrap.BaseComponent

import akka.actor.ActorSystem
import akka.dispatch.ExecutorServiceDelegate
import config.config2RichConfig

/**
 * Just needed for inheritance.
 */
abstract sealed class Concurrent

  extends BaseComponent[Concurrent]("plain-concurrent") {

  override def isStopped = actorSystem.isTerminated

  override def start = {
    if (isEnabled) {
      if (isStopped) throw new IllegalStateException("Underlying system already terminated and cannot be started more than once.")
      actorSystem
    }
    this
  }

  override def stop = {
    if (isStarted) actorSystem.shutdown
    this
  }

  override def awaitTermination(timeout: Duration) = if (!actorSystem.isTerminated) actorSystem.awaitTermination(timeout)

  lazy val dispatcher = actorSystem.dispatcher

  lazy val scheduler = actorSystem.scheduler

  /**
   * Provide access to the protected field 'executorService' of the dispatcher in order to share the nicely configured threadpool from akka.
   */
  lazy val executor = {
    import language.existentials
    val clazz = actorSystem.dispatcher.getClass
    val executorService = clazz.getDeclaredMethod("executorService")
    executorService.setAccessible(true)
    executorService.invoke(actorSystem.dispatcher).asInstanceOf[ExecutorServiceDelegate].executor
  }

  private[this] final lazy val actorSystem = {
    import config._
    import config.settings._
    ActorSystem(getString("plain.concurrent.actorsystem", "default"), config.settings)
  }

}

/**
 * The Concurrent object.
 */
object Concurrent extends Concurrent

