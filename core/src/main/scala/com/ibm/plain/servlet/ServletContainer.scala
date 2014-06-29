package com.ibm

package plain

package servlet

import java.nio.file.Path

import bootstrap.{ BaseComponent, IsSingleton, Singleton }
import io.FileExtensionFilter
import logging.Logger

/**
 *
 */
final class ServletContainer private

  extends BaseComponent[ServletContainer]("plain-servlet-container")

  with Logger

  with IsSingleton {

  final def getServletContext(path: String): ServletContext = webapplications.getOrElse(path, null)

  final def getServletContexts: Set[ServletContext] = if (null != webapplications) webapplications.values.toSet else Set.empty

  override def isStopped = null == webapplications || 0 == webapplications.size

  override def start = {
    webapplications = {
      val parentloader = Thread.currentThread.getContextClassLoader
      try {
        val applicationpaths: Array[Path] = if (webApplicationsDirectory.exists) {
          webApplicationsDirectory.listFiles(FileExtensionFilter("war")) match {
            case null  ⇒ Array.empty
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
    debug((if (0 < webapplications.size) " Servlet applications: " + webapplications.keySet.mkString(", ") else "No servlet applications."))
    this
  }

  override def stop = {
    webapplications.values.foreach(ctx ⇒ ignore(ctx.destroy))
    webapplications = null
    this
  }

  private[this] final var webapplications: Map[String, ServletContext] = null

}

/**
 *
 */
object ServletContainer

  extends Singleton[ServletContainer](new ServletContainer)
