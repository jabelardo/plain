package com.ibm.plain

package lib

package crypt

/**
 *
 */
private[crypt] abstract class DigestMethod(method: String) {

  def apply(bytes: Array[Byte]) = Digest.apply(bytes, method)

  def apply(s: String) = Digest.apply(s, method)

}

/**
 * Simple wrapper around [java.security.MessageDigest].
 */
object Digest {

  def apply(bytes: Array[Byte], method: String): String = {
    val md5 = java.security.MessageDigest.getInstance(method)
    md5.reset()
    md5.update(bytes)
    md5.digest().map(0xff & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

  def apply(s: String, method: String): String = apply(s.getBytes(text.UTF8), method)

  /**
   * Convert bytes or a string into an MD5 hash simply by calling MD5(...).
   */
  object MD5 extends DigestMethod("MD5")

  /**
   * Convert bytes or a string into an SHA-1 hash simply by calling SHA1(...).
   */
  object SHA1 extends DigestMethod("SHA-1")

  /**
   * Convert bytes or a string into an SHA-256 hash simply by calling SHA256(...).
   */
  object SHA256 extends DigestMethod("SHA-256")

}

