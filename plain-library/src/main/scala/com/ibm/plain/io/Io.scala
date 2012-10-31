package com.ibm

package plain

package io

import java.io.File

import scala.collection.mutable.{ HashSet, SynchronizedSet }
import scala.concurrent.duration._

import org.apache.commons.io.FileUtils

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Io

  extends BaseComponent[Io]("plain-io") {

  override def stop = {
    if (isStarted) deleteAll
    this
  }

  def add(file: File) = files += file

  private[this] def deleteAll = {
    files.filter(!_.isDirectory).foreach(_.delete)
    files.filter(_.isDirectory).foreach(FileUtils.deleteDirectory)
    files.clear
  }

  private[this] val files = new HashSet[File] with SynchronizedSet[File]

}

/**
 * The Io object.
 */
object Io extends Io

