package com.ibm

package plain

package concurrent

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * If two or more threads try to execute p simultaneously only one will succeed, the others block until p has finished
 * any further calls to p will simply skip p. There can only be one call to 'onlyonce' in a class extending this trait.
 */
trait OnlyOnce {

  /**
   * May be used at only one location in the extending class, but called many times and from different threads, of course.
   */
  final def onlyonce(p: ⇒ Unit) = {
    if (!done.get) {
      try {
        doing.lock
        if (!done.get) {
          p;
          done.set(true)
        }
      } finally {
        doing.unlock
      }
    }
  }

  private[this] final val done = new AtomicBoolean(false)

  private[this] final val doing = new ReentrantLock

}
