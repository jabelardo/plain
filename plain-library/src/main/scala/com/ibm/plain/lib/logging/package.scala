package com.ibm.plain

package lib

import scala.collection.JavaConversions.asScalaBuffer

import config.settings.{ getConfig, getString, getStringList }

package object logging

  extends config.CheckedConfig {

  import config._
  import config.settings._

  final def createLogger(any: Any) = Logging.createLogger(any)

  final def createLogger(name: String) = Logging.createLogger(name)

  final def defaultLogger = Logging.createLogger(Logging.name)

  final val loggingLevel = {
    val level = getString("plain.logging.level").toUpperCase
    System.setProperty("rootLevel", level match {
      case "WARNING" ⇒ "WARN"
      case v ⇒ v
    })
    level
  }

  final val loggingConsole = new LogSettings("plain.logging.console")

  final val loggingText = new LogSettings("plain.logging.text")

  final val loggingHtml = new LogSettings("plain.logging.html")

  final val filterDebugNames: List[String] = try {
    getStringList("plain.logging.filter-debug-names").toList
  } catch {
    case _: Throwable ⇒ List.empty
  }

  final class LogSettings(path: String) {

    private[this] val cfg = getConfig(path)

    final val enable = cfg.getBoolean("enable")

    System.setProperty(path + ".enable", enable.toString)

    final val toFile = if (enable) cfg.getString("file") match {
      case "" | "." | null ⇒ false
      case v ⇒
        System.setProperty(path + ".file", v)
        true
    }
    else false

    if (enable) cfg.getString("pattern") match {
      case "" | null ⇒
      case v ⇒ System.setProperty(path + ".pattern", v)
    }

    if (enable) cfg.getString("rolling-pattern") match {
      case "" | null ⇒
      case v ⇒ System.setProperty(path + ".rolling-pattern", v)
    }

  }

}
