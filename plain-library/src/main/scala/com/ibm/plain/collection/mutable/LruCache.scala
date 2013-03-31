package com.ibm

package plain

package collection

package mutable

import scala.collection.JavaConversions.collectionAsScalaIterable

import com.googlecode.concurrentlinkedhashmap.{ EvictionListener, ConcurrentLinkedHashMap }

case class LruCache[A](

  maxcapacity: Int = 500,

  initialcapacity: Int = 16) {

  def onRemove(elem: A): Unit = ()

  final def get(key: Any) = Option(store.get(key))

  final def remove(key: Any) = Option(store.remove(key))

  final def clear = { store.values.foreach(onremove); store.clear }

  final def size = store.size

  /**
   * Removes the previously stored value or null.
   */
  final def add(key: Any, value: A): A = store.putIfAbsent(key, value)

  private[this] final def onremove(entry: A) = onRemove(entry)

  private[this] final val store = new ConcurrentLinkedHashMap.Builder[Any, A]
    .initialCapacity(initialcapacity)
    .maximumWeightedCapacity(maxcapacity)
    .listener(new EvictionListener[Any, A] { def onEviction(k: Any, v: A) = onremove(v) })
    .build

}
