package com.ibm.plain

package lib

import java.io.{ File, IOException, InputStream, OutputStream, Reader, Writer }
import java.nio.ByteBuffer
import java.nio.channels.{ FileChannel, ReadableByteChannel, WritableByteChannel }
import java.nio.channels.Channels.newChannel
import java.nio.file.{ Files, Paths }
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions.collectionAsScalaIterable

import org.apache.commons.io.FileUtils

import lib.config.CheckedConfig

import config.config2RichConfig
import config.settings.{ getInt, getMilliseconds }

import logging.createLogger
import concurrent.{ addShutdownHook, spawn }

package object aio

  extends CheckedConfig {

  import config._
  import config.settings._

  /**
   * If not set differently this will result to 2k which proved to provide best performance under high load.
   */
  final val defaultBufferSize = getBytes("plain.io.default-buffersize", 2 * 1024).toInt

}
