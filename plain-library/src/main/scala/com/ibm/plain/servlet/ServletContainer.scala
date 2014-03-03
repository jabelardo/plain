package com.ibm

package plain

package servlet

import java.nio.file.Path

import com.ibm.plain.bootstrap.BaseComponent
import com.ibm.plain.io.FileExtensionFilter

import logging.createLogger

/**
 *
 */
abstract sealed class ServletContainer

  extends BaseComponent[ServletContainer]("ServletContainer") {

  final def getServletContext(path: String): ServletContext = webapplications.getOrElse(path, null)

  final def getServletContexts: Set[ServletContext] = if (null != webapplications) webapplications.values.toSet else Set.empty

  override def isStopped = null == webapplications || 0 == webapplications.size

  override def start = {
    webapplications = {
      val parentloader = Thread.currentThread.getContextClassLoader
      try {
        val applicationpaths: Array[Path] = if (webApplicationsDirectory.exists) {
          webApplicationsDirectory.listFiles(FileExtensionFilter("war")) match {
            case null ⇒ Array.empty
            case files ⇒ files.map { f ⇒ webApplicationsDirectory.toPath.resolve(f.getName).toAbsolutePath }
          }
        } else Array.empty
        applicationpaths.map(_.toString).map { applicationpath ⇒
          try {
            val classloader = ServletClassLoader(applicationpath, parentloader, unpackWebApplicationsDirectory.getAbsolutePath)
            Thread.currentThread.setContextClassLoader(classloader)
            val servletcontext = new ServletContext(classloader, applicationpath)
            (servletcontext.getContextPath, servletcontext)
          } finally Thread.currentThread.setContextClassLoader(parentloader)
        }.toMap
      } finally Thread.currentThread.setContextClassLoader(parentloader)
    }
    createLogger(this).debug(name + " has started. " + (if (0 < webapplications.size) " Servlet applications: " + webapplications.keySet.mkString(", ") else "No servlet applications."))
    this
  }

  override def stop = try {
    webapplications.values.foreach(ctx ⇒ ignore(ctx.destroy))
    webapplications = null
    createLogger(this).debug(name + " has stopped.")
    this
  }

  private[this] final var webapplications: Map[String, ServletContext] = null

}

/**
 *
 */
object ServletContainer

  extends ServletContainer
