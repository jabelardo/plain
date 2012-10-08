package com.ibm.plain

package lib

package monitor

import scala.collection.JavaConversions._

import akka.event.Logging.{ DebugLevel, ErrorLevel, InfoLevel, WarningLevel }

/**
 * Implementation of a simple Monitor to manage a 'plain' application.
 */
class Monitor {

  def getConfiguration: Array[String] = _config

  def getFreeMemoryMb = (Runtime.getRuntime.freeMemory / (1024 * 1024)).toInt

  def getMaxMemoryMb = (Runtime.getRuntime.maxMemory / (1024 * 1024)).toInt

  def getTotalMemoryMb = (Runtime.getRuntime.totalMemory / (1024 * 1024)).toInt

  def getLogLevel = logging.loggingSystem.eventStream.logLevel match {
    case DebugLevel ⇒ "Debug"
    case InfoLevel ⇒ "Info"
    case WarningLevel ⇒ "Warning"
    case ErrorLevel ⇒ "Error"
    case l ⇒ l.toString
  }

  def setLogLevel(level: String) = logging.loggingSystem.eventStream.setLogLevel(level match {
    case "Debug" ⇒ DebugLevel
    case "Info" ⇒ InfoLevel
    case "Warning" ⇒ WarningLevel
    case "Error" ⇒ ErrorLevel
    case _ ⇒ logging.loggingSystem.eventStream.logLevel
  })

  def getUptimeInSeconds = concurrent.actorSystem.uptime

  def shutdown(token: String): String = if (shutdownToken == token) {
    concurrent.shutdown
    "Shutting down now."
  } else throw new IllegalArgumentException("Invalid token.")

  private[this] final lazy val _config =
    config.settings.entrySet.map(e ⇒ e.getKey + "=" + e.getValue.render).toArray.sorted

}

