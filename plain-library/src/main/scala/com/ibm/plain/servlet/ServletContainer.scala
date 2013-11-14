package com.ibm

package plain

package servlet

import bootstrap.BaseComponent
import logging.HasLogger

import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._

import _root_.com.ibm.plain.io.FileExtensionFilter
import _root_.com.ibm.plain.io.WarClassLoader.setAsContextClassLoader

abstract sealed class ServletContainer

  extends BaseComponent[ServletContainer]("ServletContainer")

  with HasLogger {

  override def isStopped = null == webapplications

  override def start = {
    webapplications = {
      val context = Thread.currentThread.getContextClassLoader
      Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, context) // move to JSP
      try {
        val apps: Array[Path] = if (webApplicationsDirectory.exists) {
          webApplicationsDirectory.listFiles(FileExtensionFilter("war")) match {
            case null ⇒ Array.empty
            case files ⇒ files.map { f ⇒ webApplicationsDirectory.toPath.resolve(f.getName).toAbsolutePath }
          }
        } else Array.empty
        apps.map { app ⇒
          Thread.currentThread.setContextClassLoader(context)
          val servletcontext = new ServletContext(setAsContextClassLoader(app.toString, unpackWebApplicationsDirectory.getAbsolutePath))
          (servletcontext.getApplicationName, servletcontext)
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
