package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.nio.charset.Charset

import scala.collection.immutable.Map
import scala.collection.mutable.HashMap

/**
 * A cheater. Just to save a few nanoseconds.
 */
object StringPool {

  final val maxStringLength = 128

  final def arraySize = maxStringLength

  final def get(array: Array[Byte], length: Int, characterset: Charset): String = {
    strings.get(hash(array, length)) match {
      case Some(s) if length <= maxLength ⇒ s
      case _ ⇒ new String(array, 0, length, characterset)
    }
  }

  private[this] final val strings: Map[Int, String] = {
    val map = new HashMap[Int, String]
    val buf = ByteBuffer.wrap(new Array[Byte](maxStringLength))

    def add(s: String) = {
      buf.clear
      buf.put(s.getBytes)
      map.put(hash(buf.array, buf.position), s) match {
        case Some(_) ⇒ throw new IllegalArgumentException("Keyword with identical hash value found : " + s)
        case _ ⇒
      }
    }

    add(" ")
    add("  ")
    add("   ")
    add("    ")
    add("     ")
    add("      ")
    add("\t")
    add("GET")
    add("HEAD")
    add("POST")
    add("PUT")
    add("DELETE")
    add("OPTIONS")
    add("TRACE")
    add("HTTP/1.1")
    add("HTTP/1.0")
    add("Server")
    add("server")
    add("Date")
    add("date")
    add("Content-Type")
    add("Content-Encoding")
    add("connection")
    add("Keep-Alive")
    add("Keep-alive")
    add("keep-alive")
    add("User-Agent")
    add("User-agent")
    add("user-agent")
    add("curl/7.30.0")
    add("Wget/1.14 (darwin12.1.0)")
    add("Host")
    add("host")
    add("Expect")
    add("expect")
    add("Accept")
    add("accept")
    add("Accept-Encoding")
    add("Accept-encoding")
    add("accept-encoding")
    add("Accept-Language")
    add("Accept-language")
    add("accept-language")
    add("gzip, deflate")
    add("en-us,en;q=0.5")
    add("*/*")
    add("127.0.0.1:80")
    add("127.0.0.1:8000")
    add("127.0.0.1:8080")
    add("127.0.0.1:9080")
    add("localhost:80")
    add("localhost:8000")
    add("localhost:8080")
    add("localhost:9080")
    add("plaintext")
    add("json")
    add("fortunes")
    add("db")
    add("update")
    add("text/plain,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7")

    map.toMap
  }

  @inline private[this] final def hash(array: Array[Byte], length: Int): Int = {
    var h = 1
    var i = 0
    while (i < length) { h = h * 31 + array(i); i += 1 }
    h
  }

  private[this] final val maxLength = strings.values.maxBy(_.length).length

}

