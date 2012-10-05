package com.ibm.plain

package lib

package collection

package mutable

import scala.collection.JavaConversions.collectionAsScalaIterable

import com.googlecode.concurrentlinkedhashmap.{ EvictionListener, ConcurrentLinkedHashMap }

case class LruCache[T](

  maxcapacity: Int = 500,

  initialcapacity: Int = 16) {

  def onRemove(elem: T): Unit = ()

  def get(key: Any) = Option(store.get(key))

  def remove(key: Any) = Option(store.remove(key))

  def clear = { store.values.foreach(onremove(_)); store.clear }

  def add(key: Any, value: T) = store.putIfAbsent(key, value)

  private[this] def onremove(entry: T) = onRemove(entry)

  private[this] final val store = new ConcurrentLinkedHashMap.Builder[Any, T]
    .initialCapacity(initialcapacity)
    .maximumWeightedCapacity(maxcapacity)
    .listener(new EvictionListener[Any, T] { def onEviction(k: Any, v: T) = onremove(v) })
    .build()

}
