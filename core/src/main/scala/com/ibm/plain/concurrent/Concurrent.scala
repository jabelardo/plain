package com.ibm

package plain

package concurrent

import java.util.concurrent.{ Executors, ForkJoinPool, TimeUnit }

import com.ibm.plain.bootstrap.{ BaseComponent, Singleton }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import bootstrap.{ BaseComponent, IsSingleton, Singleton }

/**
 *
 */
final class Concurrent private

  extends BaseComponent[Concurrent]("plain-concurrent")

  with OnlyOnce

  with IsSingleton {

  override final def stop = {
    if (isStarted) {
      forkjoinpool.shutdown
      scheduledpool.shutdown
      ignore(Thread.sleep(bootstrap.delayDuringTeardown))
      forkjoinpool.shutdownNow
      scheduledpool.shutdownNow
    }
    this
  }

  override final def awaitTermination(timeout: Duration) = ignore(forkjoinpool.awaitTermination(timeout.toMillis, TimeUnit.MILLISECONDS))

  final def scheduler = scheduledpool

  final def executor = executioncontext

  final def ioexecutor = forkjoinpool

  private[this] final val scheduledpool = Executors.newScheduledThreadPool(cores)

  private[this] final val forkjoinpool = new ForkJoinPool(parallelism)

  private[this] final val executioncontext: ExecutionContext = ExecutionContext.fromExecutorService(forkjoinpool)

}

/**
 * The Concurrent object.
 */
object Concurrent

  extends Singleton[Concurrent](new Concurrent)
