package com.ibm

package plain

package servlet

import java.io.{ ByteArrayOutputStream ⇒ JByteArrayOutputStream, File, InputStream }
import java.net.{ URL, URLClassLoader }
import java.nio.file.Paths

import org.apache.commons.io.{ FileUtils, FilenameUtils }
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.{ FileHeader, UnzipParameters }

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

import io.{ ByteArrayInputStream, copyBytes, defaultBufferSize, temporaryDirectory }
import logging.Logger

/**
 *
 */
final class ServletClassLoader private (

  name: String,

  urls: Array[URL],

  parent: ClassLoader,

  unpackdirectory: File)

    extends URLClassLoader(urls, parent)

    with Logger {

  override final def toString = name

  override final def getResourceAsStream(name: String): InputStream = resourcesnotfound.get(name) match {
    case Some(_) ⇒ null
    case _ ⇒ resourcesloaded.get(name) match {
      case Some(array) ⇒ new ByteArrayInputStream(array)
      case _ ⇒ super.getResourceAsStream(name) match {
        case null ⇒
          resourcesnotfound.put(name, null)
          null
        case in ⇒ try {
          val out = new JByteArrayOutputStream(defaultBufferSize)
          val array = try {
            copyBytes(in, out)
            out.toByteArray
          } finally out.close
          if (!name.endsWith(CLASSFILESUFFIX)) resourcesloaded.put(name, array)
          new ByteArrayInputStream(array)
        } catch {
          case _: Throwable ⇒ null
        } finally in.close
      }
    }
  }

  override final def loadClass(name: String, resolve: Boolean): Class[_] = findClass(name) match {
    case c ⇒ if (resolve) resolveClass(c); c
  }

  override final def findClass(name: String): Class[_] = findLoadedClass0(name) match {
    case null ⇒ ignoreOrElse(systemloader.loadClass(name), null) match {
      case null ⇒ findLocalClass(name) match {
        case null ⇒
          getParent match {
            case null ⇒
              classesnotfound.put(name, null)
              throw new ClassNotFoundException(name)
            case parent ⇒ parent.loadClass(name)
          }
        case c ⇒
          classesloaded.put(name, c)
          c
      }
      case systemclass ⇒ systemclass
    }
    case loadedclass ⇒ loadedclass
  }

  @inline final protected def findLoadedClass0(name: String): Class[_] = classesloaded.get(name) match {
    case Some(c) ⇒ c
    case _ ⇒ findLoadedClass(name)
  }

  @inline final protected def findLocalClass(name: String): Class[_] = {
    val classfilepath = name.replace(".", "/") + CLASSFILESUFFIX
    directoryurls.exists(_.resolve(classfilepath).toFile.exists) match {
      case true ⇒ getResourceAsStream(classfilepath) match {
        case null ⇒ null
        case in: ByteArrayInputStream ⇒
          defineClass(null, in.getByteArray, 0, in.available) match {
            case c ⇒
              classesloaded.put(name, c)
              c
          }
      }
      case _ ⇒ null
    }
  }

  override final def close = {
    resourcesloaded.clear
    resourcesnotfound.clear
    classesloaded.clear
    classesnotfound.clear
    super.close
  }

  private[this] final val resourcesloaded = new TrieMap[String, Array[Byte]]

  private[this] final val resourcesnotfound = new TrieMap[String, Null]

  private[this] final val classesloaded = new TrieMap[String, Class[_]]

  private[this] final val classesnotfound = new TrieMap[String, Null]

  private[this] final val systemloader = ClassLoader.getSystemClassLoader

  private[this] final val directoryurls = getURLs.map(_.toURI).filter(_.getPath().endsWith("/")).map(Paths.get(_))

  private[this] final val CLASSFILESUFFIX = ".class"

}

/**
 *
 */
object ServletClassLoader

    extends Logger {

  final def apply(source: String, parent: ClassLoader): URLClassLoader = apply(source, parent, temporaryDirectory.getAbsolutePath)

  final def apply(source: String, parent: ClassLoader, directory: String): URLClassLoader = {
    val sourcepath = Paths.get(source).toFile.getAbsoluteFile
    val sourcewithoutextension = FilenameUtils.normalize(FilenameUtils.removeExtension(sourcepath.getName))
    val target = Paths.get(directory).resolve(sourcewithoutextension).toFile.getAbsoluteFile
    val libdir = target.toPath.resolve("WEB-INF/lib").toFile
    val classesdir = target.toPath.resolve("WEB-INF/classes").toFile
    val metainfdir = target.toPath.resolve("META-INF").toFile
    val webinfdir = target.toPath.resolve("WEB-INF").toFile
    val urls = new ListBuffer[File]
    def unpackJars = if (libdir.exists) {
      val total = FileUtils.listFiles(libdir, Array("jar"), true).size
      val unzipparameters = new UnzipParameters
      unzipparameters.setIgnoreAllFileAttributes(true)
      var i = 0
      FileUtils.listFiles(libdir, Array("jar"), true).foreach { libfile ⇒
        i += 1
        trace("Extract " + i + " of " + total + " (" + libfile.getName + ")")
        val zipfile = new ZipFile(libfile.getAbsolutePath)
        zipfile.getFileHeaders.map(_.asInstanceOf[FileHeader]).
          filter(!_.getFileName.toLowerCase.contains("license")).
          foreach { file ⇒
            try zipfile.extractFile(file, classesdir.getAbsolutePath)
            catch { case e: Throwable ⇒ warn("Could not unpack file " + file.getFileName + " in " + libfile.getName + " : " + e.getMessage) }
          }
        libfile.delete
      }
    }
    if (forceUnpackWebApplications || sourcepath.lastModified > target.lastModified) {
      info("Extract " + sourcepath + " to " + target)
      FileUtils.deleteDirectory(target)
      new ZipFile(sourcepath).extractAll(target.getAbsolutePath)
      if (unpackWebApplicationsRecursively) unpackJars
    }
    if (libdir.exists) urls ++= libdir.listFiles
    if (classesdir.exists) urls += classesdir
    if (metainfdir.exists) urls += metainfdir
    if (webinfdir.exists) urls += webinfdir
    urls += target
    new ServletClassLoader(sourcewithoutextension, urls.map(_.toPath.toUri.normalize.toURL).toArray, parent, target)
  }

}
