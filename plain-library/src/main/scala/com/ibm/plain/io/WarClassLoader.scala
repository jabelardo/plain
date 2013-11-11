package com.ibm

package plain

package io

import java.io.{ File, InputStream }
import java.net.{ URL, URLClassLoader }
import java.nio.file.Paths

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.io.Source.fromInputStream
import scala.language.postfixOps
import scala.xml.XML

import org.apache.commons.io.{ FileUtils, FilenameUtils }

import net.lingala.zip4j.core.ZipFile

final class WarClassLoader private (

  name: String,

  urls: Array[URL], parent: ClassLoader)

  extends URLClassLoader(urls, parent) {

  override final def toString = name

  override final def getResourceAsStream(name: String): InputStream = cache.get(name) match {
    case Some(array) ⇒ new ByteArrayInputStream(array)
    case _ ⇒ super.getResourceAsStream(name) match {
      case null ⇒ null
      case in ⇒ try {
        val file = new File(getResource(name).toURI)
        val array = new Array[Byte](file.length.toInt)
        val out = new ByteArrayOutputStream(array)
        copyBytesIo(in, out)
        cache.put(name, array)
        new ByteArrayInputStream(array)
      } catch {
        case _: Throwable ⇒ null
      } finally in.close
    }
  }

  override final def loadClass(name: String, resolve: Boolean) = super.loadClass(name, resolve)

  private[this] final val cache = new TrieMap[String, Array[Byte]]

}

object WarClassLoader {

  final def setAsContextClassLoader(source: String): URLClassLoader = setAsContextClassLoader(source, temporaryDirectory.getAbsolutePath)

  final def setAsContextClassLoader(source: String, directory: String): URLClassLoader = {
    val parent = Thread.currentThread.getContextClassLoader
    val loader = apply(source, parent, directory)
    Thread.currentThread.setContextClassLoader(loader)
    loader
  }

  final def apply(source: String, parent: ClassLoader): URLClassLoader = apply(source, parent, temporaryDirectory.getAbsolutePath)

  final def apply(sourcepath: String, parent: ClassLoader, directory: String): URLClassLoader = {
    val source = Paths.get(sourcepath).toFile.getAbsoluteFile
    var withoutextension = FilenameUtils.removeExtension(source.getName)
    val target = Paths.get(directory).resolve(withoutextension).toFile.getAbsoluteFile
    if (source.lastModified > target.lastModified) {
      FileUtils.deleteDirectory(target)
      val zipfile = new ZipFile(sourcepath)
      zipfile.extractAll(target.getAbsolutePath)
    }
    val urls = new ListBuffer[File]
    val classesdir = target.toPath.resolve("WEB-INF/classes").toFile
    urls += target
    urls += classesdir
    urls ++= target.toPath.resolve("WEB-INF/lib").toFile.listFiles
    val loader = new WarClassLoader(withoutextension, urls.map(_.toPath.toUri.toURL).toArray, parent)
    FileUtils.listFiles(classesdir, Array("class"), true).map(c ⇒ classesdir.toPath.relativize(c.toPath).toString.replace("/", ".").replace(".class", "")).foreach(loader.loadClass(_, true))
    loader
  }

}

object Test1 extends App {

  import WarClassLoader._
  val cl = setAsContextClassLoader("/Users/guido/Development/Others/dashboard-demo/target/quicktickets-dashboard-1.0.1.war", "/tmp/web-apps")
  val a = cl.loadClass("com.vaadin.ui.Component")
  val b = Class.forName("com.vaadin.demo.dashboard.HelpOverlay", true, cl).newInstance
  val f = Class.forName("org.apache.james.mime4j.codec.CodecUtil", true, cl).newInstance
  println(b + " " + f)
  val c = "WEB-INF/web.xml"
  val d = "VAADIN/widgetsets/WEB-INF/deploy/com.vaadin.demo.dashboard.DashboardWidgetSet/symbolMaps/AC942BD1CD918B3B58EE8569A3FF9707.symbolMap"
  for (i ← 1 to 3) println(fromInputStream(cl.getResourceAsStream(c)).getLines.mkString)
  for (i ← 1 to 3) println(fromInputStream(cl.getResourceAsStream(d)).getLines.mkString.length)

  val web = XML.load(cl.getResourceAsStream(c))
  println(web)
  println((web \ "servlet"))
  println((web \ "display-name").text)
  println((web \ "welcome-file-list" \ "welcome-file").map(_.text))
  println(cl.getURLs.toList)
  import scala.collection.JavaConversions._
  println(cl.findResources("").toList)
  println(cl.findResources("images/ant_logo_large.gif").toList)
  println(cl.findResources("META-INF/NOTICE.txt").toList)

  val cookie = new java.net.HttpCookie("Name", "Guido")
  cookie.setDomain("ibm.com")
  cookie.setPath("/hello")
  cookie.setVersion(1)
  cookie.setMaxAge(10000)
  println(cookie)

}
