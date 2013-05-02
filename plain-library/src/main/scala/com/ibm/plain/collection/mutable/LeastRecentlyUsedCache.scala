package com.ibm

package plain

package collection

package mutable

import scala.collection.JavaConversions.collectionAsScalaIterable

import com.googlecode.concurrentlinkedhashmap.{ EvictionListener, ConcurrentLinkedHashMap }

final class LeastRecentlyUsedCache[@specialized A] private (

  maxcapacity: Int,

  initialcapacity: Int) {

  final def setOnRemove(f: A ⇒ Unit): Unit = onremove = f

  final def get(key: Any) = Option(store.get(key))

  final def remove(key: Any) = Option(store.remove(key))

  final def clear = { store.values.foreach(onremove); store.clear }

  final def size = store.size

  /**
   * Removes the previously stored value or null.
   */
  final def put(key: Any, value: A): A = store.putIfAbsent(key, value)

  private[this] final val store = new ConcurrentLinkedHashMap.Builder[Any, A]
    .initialCapacity(initialcapacity)
    .maximumWeightedCapacity(maxcapacity)
    .listener(new EvictionListener[Any, A] { def onEviction(k: Any, v: A) = { onremove(v) } })
    .build

  private[this] final var onremove: A ⇒ Unit = (a: A) ⇒ ()

}

/**
 *
 */
object LeastRecentlyUsedCache {

  final def apply[A](maxcapacity: Int, initialcapacity: Int) = new LeastRecentlyUsedCache[A](maxcapacity, initialcapacity)

  final def apply[A](maxcapacity: Int): LeastRecentlyUsedCache[A] = apply(maxcapacity, 0)

  final def apply[A]: LeastRecentlyUsedCache[A] = apply(1024, 0)

}