package com.ibm

package plain

package io

import java.io.File

import org.apache.commons.io.FileUtils

import scala.collection.concurrent.TrieMap

import bootstrap.{ BaseComponent, IsSingleton, Singleton }

/**
 *
 */
final class Io private

    extends BaseComponent[Io]("plain-io")

    with IsSingleton {

  override def stop = {
    if (isStarted) deleteAll
    this
  }

  def add(file: File) = files.put(file, null)

  private[this] def deleteAll = {
    files.keySet.filter(!_.isDirectory).foreach(_.delete)
    files.keySet.filter(_.isDirectory).foreach(FileUtils.deleteDirectory)
    files.clear
  }

  private[this] val files = new TrieMap[File, Null]

}

/**
 * The Io object.
 */
object Io

  extends Singleton[Io](new Io)

