package com.ibm

package plain

package servlet

import java.nio.file.Paths

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._

import bootstrap.BaseComponent
import logging.HasLogger
import io.FileExtensionFilter
import io.WarClassLoader._

abstract sealed class ServletContainer

  extends BaseComponent[ServletContainer]("ServletContainer")

  with HasLogger {

  override def isStopped = null == webapplications

  override def start = {
    webapplications = {
      val context = Thread.currentThread.getContextClassLoader
      try {
        val apps = (webApplicationsDirectory.listFiles(FileExtensionFilter("war")) match {
          case null ⇒ Array.empty
          case files ⇒ files.map { f ⇒ webApplicationsDirectory.toPath.resolve(f.getName).toAbsolutePath }
        }).toList
        apps.map { app ⇒
          Thread.currentThread.setContextClassLoader(context)
          val servletcontext = new ServletContext(setAsContextClassLoader(app.toString, unpackWebApplicationsDirectory.getAbsolutePath))
          (servletcontext.getApplicationName, servletcontext)
        }.toMap
      } finally Thread.currentThread.setContextClassLoader(context)
    }
    debug(webapplications.toString)
    debug(name + " has started.")
    this
  }

  override def stop = try {
    webapplications.values.foreach(ctx ⇒ ignore(ctx.destroy))
    webapplications = null
    this
  }

  final def getServletContext(path: String): Option[ServletContext] = if (isStarted) webapplications.get(path) else None

  private[this] final var webapplications: Map[String, ServletContext] = null

}

object ServletContainer extends ServletContainer
