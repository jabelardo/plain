package com.ibm

package plain

package aio

import java.util.Arrays
import java.nio.charset.Charset
import java.nio.ByteBuffer

import scala.collection.mutable.OpenHashMap

/**
 *
 */
object StringPool {

  final def get(array: Array[Byte], length: Int)(implicit cset: Charset): String = {
    strings.get(hash(array, length)) match {
      case Some(s) ⇒ s
      case _ ⇒ new String(array, 0, length, cset)
    }
  }

  private[this] final val strings = {
    val map = new OpenHashMap[Int, String]
    val buf = ByteBuffer.wrap(new Array[Byte](arraySize))

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
    add("POST")
    add("PUT")
    add("HTTP/1.1")
    add("HTTP/1.0")
    add("Connection")
    add("Keep-Alive")
    add("User-Agent")
    add("Host")
    add("Accept")
    add("*/*")
    add("ping")
    add("ApacheBench/2.3")
    add("127.0.0.1")
    add("127.0.0.1:80")
    add("127.0.0.1:8080")
    add("127.0.0.1:7500")

    map
  }

  @inline private[this] final def hash(array: Array[Byte], length: Int): Int = {
    var h = 1
    var i = 0
    while (i < length) { h = h * 31 + array(i); i += 1 }
    h
  }

  final val arraySize = 32 // must be the same value as used in Io

}

