package com.ibm.plain

package lib

package concurrent

/**
 * If two or more threads try to execute p simultaneously only one will succeed, the others block until p has finished
 * any further calls to p will simply skip p. There can only be one call to 'onlyonce' in a class extending this trait.
 */
trait OnlyOnce {

  /**
   * May be used at only one location in the extending class, but called many times and from different threads, of course.
   */
  def onlyonce(p: ⇒ Unit) = {
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

  private val done = new java.util.concurrent.atomic.AtomicBoolean
  private val doing = new java.util.concurrent.locks.ReentrantLock

}
