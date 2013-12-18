package com.ibm

package plain

package servlet

import java.nio.file.Path

import com.ibm.plain.bootstrap.BaseComponent
import com.ibm.plain.io.{ FileExtensionFilter, WarClassLoader }

import logging.HasLogger

abstract sealed class ServletContainer

  extends BaseComponent[ServletContainer]("ServletContainer")

  with HasLogger {

  override def isStopped = null == webapplications || 0 == webapplications.size

  override def start = {
    webapplications = {
      val context = Thread.currentThread.getContextClassLoader
      try {
        val apps: Array[Path] = if (webApplicationsDirectory.exists) {
          webApplicationsDirectory.listFiles(FileExtensionFilter("war")) match {
            case null ⇒ Array.empty
            case files ⇒ files.map { f ⇒ webApplicationsDirectory.toPath.resolve(f.getName).toAbsolutePath }
          }
        } else Array.empty
        apps.map { app ⇒
          try {
            val classloader = WarClassLoader(app.toString, Thread.currentThread.getContextClassLoader, unpackWebApplicationsDirectory.getAbsolutePath)
            Thread.currentThread.setContextClassLoader(classloader)
            val servletcontext = new ServletContext(classloader)
            (servletcontext.getApplicationName, servletcontext)
          } finally Thread.currentThread.setContextClassLoader(context)
        }.toMap
      } finally Thread.currentThread.setContextClassLoader(context)
    }
    debug(name + " has started. (" + webapplications.keySet.mkString(", ") + ")")
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
