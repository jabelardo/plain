package com.ibm.plain

package lib

package io

import java.io.File

import scala.collection.mutable.{ HashSet, SynchronizedSet }
import scala.concurrent.util.Duration

import org.apache.commons.io.FileUtils

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Io

  extends BaseComponent[Io]("plain-io") {

  def isStarted = true

  def isStopped = false

  def start = this

  def stop = {
    if (isStarted) deleteAll
    this
  }

  def awaitTermination(timeout: Duration) = ()

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

