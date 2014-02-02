package com.ibm

package plain

package io

import java.io.{ File, InputStream, ByteArrayOutputStream ⇒ JByteArrayOutputStream }
import java.net.{ URL, URLClassLoader }
import java.nio.file.Paths

import scala.collection.JavaConversions.{ collectionAsScalaIterable, enumerationAsScalaIterator }
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.io.Source.fromInputStream
import scala.language.postfixOps
import scala.xml.XML

import org.apache.commons.io.{ FileUtils, FilenameUtils }

import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.core.ZipFile

import concurrent.spawn
import logging.HasLogger

final class WarClassLoader private (

  name: String,

  urls: Array[URL],

  parent: ClassLoader,

  unpackdirectory: File)

  extends URLClassLoader(urls, if (null == parent) ClassLoader.getSystemClassLoader else parent) {

  override final def toString = name

  override final def getResourceAsStream(name: String): InputStream = cache.get(name) match {
    case Some(array) ⇒ new ByteArrayInputStream(array)
    case _ ⇒ super.getResourceAsStream(name) match {
      case null ⇒ null
      case in ⇒ try {
        val out = new JByteArrayOutputStream(1024)
        copyBytesIo(in, out)
        val array = out.toByteArray
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

object WarClassLoader

  extends HasLogger {

  final def setAsContextClassLoader(source: String): URLClassLoader = setAsContextClassLoader(source, temporaryDirectory.getAbsolutePath)

  final def setAsContextClassLoader(source: String, directory: String): URLClassLoader = {
    val parent = Thread.currentThread.getContextClassLoader
    val loader = apply(source, parent, directory)
    Thread.currentThread.setContextClassLoader(loader)
    loader
  }

  final def apply(source: String): URLClassLoader = apply(source, Thread.currentThread.getContextClassLoader)

  final def apply(source: String, parent: ClassLoader): URLClassLoader = apply(source, parent, temporaryDirectory.getAbsolutePath)

  final def apply(source: String, parent: ClassLoader, directory: String): URLClassLoader = {
    val sourcepath = Paths.get(source).toFile.getAbsoluteFile
    val sourcewithoutextension = FilenameUtils.removeExtension(sourcepath.getName)
    val target = Paths.get(directory).resolve(sourcewithoutextension).toFile.getAbsoluteFile
    val libdir = target.toPath.resolve("WEB-INF/lib").toFile
    val classesdir = target.toPath.resolve("WEB-INF/classes").toFile
    val metainfdir = target.toPath.resolve("META-INF").toFile
    val webinfdir = target.toPath.resolve("WEB-INF").toFile
    if (sourcepath.lastModified > target.lastModified) {
      FileUtils.deleteDirectory(target)
      new ZipFile(sourcepath).extractAll(target.getAbsolutePath)
      FileUtils.listFiles(libdir, Array("jar"), true).foreach { libfile ⇒
        new ZipFile(libfile.getAbsolutePath).extractAll(classesdir.getAbsolutePath)
        libfile.delete
      }
    }
    val urls = new ListBuffer[File]
    urls += classesdir
    urls += metainfdir
    urls += webinfdir
    urls += target
    new WarClassLoader(sourcewithoutextension, urls.map(_.toPath.toUri.normalize.toURL).toArray, parent, target)
  }

}
